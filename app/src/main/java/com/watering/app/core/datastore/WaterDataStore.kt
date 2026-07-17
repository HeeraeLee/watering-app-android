package com.watering.app.core.datastore

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.watering.app.core.model.DailyAchievement
import com.watering.app.core.model.DayRecord
import com.watering.app.core.model.DrinkType
import com.watering.app.core.model.StreakInfo
import com.watering.app.core.model.WaterEntry
import com.watering.app.core.model.WaterUpdateResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "WaterDataStore"

private val Context.waterDataStore: DataStore<Preferences> by preferencesDataStore(name = "water")

@Singleton
class WaterDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: Clock
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private suspend fun editSafely(transform: suspend (MutablePreferences) -> Unit) {
        try {
            context.waterDataStore.edit(transform)
        } catch (e: Exception) {
            Log.e(TAG, "데이터 저장 실패", e)
            throw e
        }
    }

    private object Keys {
        val TODAY_RECORD = stringPreferencesKey("today_record")
        val STREAK_INFO = stringPreferencesKey("streak_info")
        val HISTORY = stringPreferencesKey("record_history")   // JSON map, 상세 90일
        val ANNUAL_HISTORY = stringPreferencesKey("annual_history")   // JSON map, 경량 365일 집계
        val LAST_UPDATED = stringPreferencesKey("last_updated")
    }

    // 알려진 한계: 이 Flow는 context.waterDataStore.data가 새로 emit될 때만 재평가되므로, 자정을
    // 넘겨도 새 기록/설정 변경이 없으면 dateKey가 갱신되지 않고 어제 값으로 멈춰있을 수 있다(Stats/
    // SmartStatsViewModel은 v0.31.22에서 별도 주기 트리거로 이 문제를 해결했으나, 이 Flow 자체와
    // 그걸 구독하는 HomeViewModel 등은 아직 범위 밖 — 필요시 후속 작업으로 동일 패턴 적용)
    val todayRecord: Flow<DayRecord> = context.waterDataStore.data.map { prefs ->
        val todayKey = LocalDate.now(clock).format(formatter)
        prefs[Keys.TODAY_RECORD]
            ?.let { runCatching { json.decodeFromString<DayRecord>(it) }.getOrNull() }
            ?.takeIf { it.dateKey == todayKey }
            ?: DayRecord(dateKey = todayKey)
    }

    val streakInfo: Flow<StreakInfo> = context.waterDataStore.data.map { prefs ->
        prefs[Keys.STREAK_INFO]
            ?.let { runCatching { json.decodeFromString<StreakInfo>(it) }.getOrNull() }
            ?: StreakInfo()
    }

    // prev/updated를 같은 edit{} 트랜잭션 안에서 캡처해서 반환한다 — DataStore의 edit은 내부적으로
    // 직렬화되므로, 여기서 읽은 prev는 위젯 연속 탭처럼 여러 호출이 겹쳐도 항상 그 트랜잭션 시점의
    // 정확한 이전 상태다(트랜잭션 밖에서 별도로 .first()로 prev를 읽으면 그 사이 다른 쓰기가 끼어들어
    // stale해질 수 있음 — WaterUpdateResult 참고).
    suspend fun addEntry(amount: Int, drinkType: DrinkType, goal: Int, cupSize: Int): WaterUpdateResult {
        val todayKey = LocalDate.now(clock).format(formatter)
        lateinit var prev: DayRecord
        lateinit var updated: DayRecord
        editSafely { prefs ->
            val current = prefs[Keys.TODAY_RECORD]
                ?.let { runCatching { json.decodeFromString<DayRecord>(it) }.getOrNull() }
                ?.takeIf { it.dateKey == todayKey }
                ?: DayRecord(dateKey = todayKey)
            prev = current
            val entry = WaterEntry(
                timestampMillis = System.currentTimeMillis(),
                amount = amount,
                drinkType = drinkType
            )
            updated = current.copy(entries = current.entries + entry, goal = goal, cupSize = cupSize)
            prefs[Keys.TODAY_RECORD] = json.encodeToString(updated)
            prefs[Keys.LAST_UPDATED] = System.currentTimeMillis().toString()
        }
        archiveTodayToHistory(updated)
        archiveToAnnualHistory(updated)
        return WaterUpdateResult(prev, updated)
    }

    suspend fun removeLastEntry(goal: Int, cupSize: Int): DayRecord {
        val todayKey = LocalDate.now(clock).format(formatter)
        lateinit var updated: DayRecord
        editSafely { prefs ->
            val current = prefs[Keys.TODAY_RECORD]
                ?.let { runCatching { json.decodeFromString<DayRecord>(it) }.getOrNull() }
                ?.takeIf { it.dateKey == todayKey }
                ?: DayRecord(dateKey = todayKey)
            updated = current.copy(entries = current.entries.dropLast(1), goal = goal, cupSize = cupSize)
            prefs[Keys.TODAY_RECORD] = json.encodeToString(updated)
        }
        // addEntry와 동일하게 취소 후 상태도 즉시 재아카이빙 — 안 하면 자정 이후 히스토리/연간
        // 집계/CSV에 취소 전 값이 영구히 남는 버그가 있었음
        archiveTodayToHistory(updated)
        archiveToAnnualHistory(updated)
        return updated
    }

    suspend fun saveStreakInfo(streak: StreakInfo) {
        editSafely { prefs ->
            prefs[Keys.STREAK_INFO] = json.encodeToString(streak)
        }
    }

    fun getHistory(): Flow<Map<String, DayRecord>> = context.waterDataStore.data.map { prefs ->
        prefs[Keys.HISTORY]
            ?.let { runCatching { json.decodeFromString<Map<String, DayRecord>>(it) }.getOrNull() }
            ?: emptyMap()
    }

    private suspend fun archiveTodayToHistory(record: DayRecord) {
        editSafely { prefs ->
            val existing = prefs[Keys.HISTORY]
                ?.let { runCatching { json.decodeFromString<Map<String, DayRecord>>(it) }.getOrNull() }
                ?: emptyMap()
            val updated = (existing + (record.dateKey to record))
                .entries.sortedByDescending { it.key }.take(90)
                .associate { it.key to it.value }
            prefs[Keys.HISTORY] = json.encodeToString(updated)
        }
    }

    // 연간 통계(히트맵)용 — 90일 상세 이력과 별개로, 엔트리 상세 없이 일별 집계만 365일치 보관한다
    // (엔트리 전체를 1년치 담으면 Preferences 쓰기마다 커지는 blob을 통째로 재저장해야 해 지연 우려가 있음)
    private suspend fun archiveToAnnualHistory(record: DayRecord) {
        editSafely { prefs ->
            val existing = prefs[Keys.ANNUAL_HISTORY]
                ?.let { runCatching { json.decodeFromString<Map<String, DailyAchievement>>(it) }.getOrNull() }
                ?: emptyMap()
            val achievement = DailyAchievement(
                dateKey = record.dateKey,
                totalCount = record.totalCount,
                goal = record.goal
            )
            val updated = (existing + (record.dateKey to achievement))
                .entries.sortedByDescending { it.key }.take(365)
                .associate { it.key to it.value }
            prefs[Keys.ANNUAL_HISTORY] = json.encodeToString(updated)
        }
    }

    fun getAnnualHistory(): Flow<Map<String, DailyAchievement>> = context.waterDataStore.data.map { prefs ->
        prefs[Keys.ANNUAL_HISTORY]
            ?.let { runCatching { json.decodeFromString<Map<String, DailyAchievement>>(it) }.getOrNull() }
            ?: emptyMap()
    }

    // goal을 받아 저장한다 — 예전엔 DayRecord()의 기본값(8)이 그대로 저장돼 사용자가 설정한
    // 목표와 다른 값이 남는 문제가 있었음. addEntry/removeLastEntry와 동일하게 재아카이빙도
    // 함께 해야 "초기화했는데 히스토리/연간 집계/CSV엔 초기화 전 값이 그대로 남는" 문제가 없음.
    suspend fun resetTodayRecord(goal: Int, cupSize: Int): DayRecord {
        val todayKey = LocalDate.now(clock).format(formatter)
        val blank = DayRecord(dateKey = todayKey, goal = goal, cupSize = cupSize)
        editSafely { prefs ->
            prefs[Keys.TODAY_RECORD] = json.encodeToString(blank)
        }
        archiveTodayToHistory(blank)
        archiveToAnnualHistory(blank)
        return blank
    }

    // 자정 롤오버 워커 전용 — 기기가 꺼져있다 늦게 켜지면 밀린 워커가 실제 자정보다 한참 뒤에
    // 실행될 수 있는데, 그 사이 위젯/앱에서 이미 오늘 날짜로 기록이 시작됐다면(addEntry의 자정
    // 자동 롤오버로 자연히 새 하루가 시작된 경우) 그 기록을 지우면 안 된다. TODAY_RECORD가 이미
    // 오늘 날짜면 손대지 않고, 아직 어제 날짜에 머물러 있을 때만 새 빈 레코드로 교체한다 —
    // 사용자가 명시적으로 요청하는 resetTodayRecord()(컵 크기 변경 "초기화")는 항상 무조건
    // 초기화해야 하므로 이 조건부 버전과 별도로 유지한다(2026-07-16, 자정 리셋 지연 시 당일
    // 기록 소실 버그 수정).
    suspend fun resetTodayRecordIfStale(goal: Int, cupSize: Int): DayRecord? {
        val todayKey = LocalDate.now(clock).format(formatter)
        var blank: DayRecord? = null
        editSafely { prefs ->
            val current = prefs[Keys.TODAY_RECORD]
                ?.let { runCatching { json.decodeFromString<DayRecord>(it) }.getOrNull() }
            if (current?.dateKey == todayKey) return@editSafely
            val fresh = DayRecord(dateKey = todayKey, goal = goal, cupSize = cupSize)
            prefs[Keys.TODAY_RECORD] = json.encodeToString(fresh)
            blank = fresh
        }
        blank?.let {
            archiveTodayToHistory(it)
            archiveToAnnualHistory(it)
        }
        return blank
    }

    suspend fun clearAllData() {
        editSafely { it.clear() }
    }

    // 백업 복원 전용 — 오늘 기록/연속 기록/히스토리/연간 집계를 클라우드 백업 스냅샷으로 전체
    // 덮어쓴다. annualHistory가 빠져있던 것을 2026-07-16에 추가 — 없으면 복원 후에도 연간
    // 히트맵이 백업 전 데이터로 남아있었음
    suspend fun restoreAll(
        today: DayRecord,
        streak: StreakInfo,
        history: Map<String, DayRecord>,
        annualHistory: Map<String, DailyAchievement>
    ) {
        editSafely { prefs ->
            prefs[Keys.TODAY_RECORD] = json.encodeToString(today)
            prefs[Keys.STREAK_INFO] = json.encodeToString(streak)
            prefs[Keys.HISTORY] = json.encodeToString(history)
            prefs[Keys.ANNUAL_HISTORY] = json.encodeToString(annualHistory)
        }
    }
}
