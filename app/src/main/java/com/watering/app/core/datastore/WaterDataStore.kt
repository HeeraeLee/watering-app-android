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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "WaterDataStore"

private val Context.waterDataStore: DataStore<Preferences> by preferencesDataStore(name = "water")

@Singleton
class WaterDataStore @Inject constructor(
    @ApplicationContext private val context: Context
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

    val todayRecord: Flow<DayRecord> = context.waterDataStore.data.map { prefs ->
        val todayKey = LocalDate.now().format(formatter)
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

    suspend fun addEntry(amount: Int, drinkType: DrinkType, goal: Int): DayRecord {
        val todayKey = LocalDate.now().format(formatter)
        lateinit var updated: DayRecord
        editSafely { prefs ->
            val current = prefs[Keys.TODAY_RECORD]
                ?.let { runCatching { json.decodeFromString<DayRecord>(it) }.getOrNull() }
                ?.takeIf { it.dateKey == todayKey }
                ?: DayRecord(dateKey = todayKey)
            val entry = WaterEntry(
                timestampMillis = System.currentTimeMillis(),
                amount = amount,
                drinkType = drinkType
            )
            updated = current.copy(entries = current.entries + entry, goal = goal)
            prefs[Keys.TODAY_RECORD] = json.encodeToString(updated)
            prefs[Keys.LAST_UPDATED] = System.currentTimeMillis().toString()
        }
        archiveTodayToHistory(updated)
        archiveToAnnualHistory(updated)
        return updated
    }

    suspend fun removeLastEntry(goal: Int): DayRecord {
        val todayKey = LocalDate.now().format(formatter)
        lateinit var updated: DayRecord
        editSafely { prefs ->
            val current = prefs[Keys.TODAY_RECORD]
                ?.let { runCatching { json.decodeFromString<DayRecord>(it) }.getOrNull() }
                ?.takeIf { it.dateKey == todayKey }
                ?: DayRecord(dateKey = todayKey)
            updated = current.copy(entries = current.entries.dropLast(1), goal = goal)
            prefs[Keys.TODAY_RECORD] = json.encodeToString(updated)
        }
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

    suspend fun resetTodayRecord() {
        val todayKey = LocalDate.now().format(formatter)
        editSafely { prefs ->
            prefs[Keys.TODAY_RECORD] = json.encodeToString(DayRecord(dateKey = todayKey))
        }
    }

    suspend fun clearAllData() {
        editSafely { it.clear() }
    }

    // 백업 복원 전용 — 오늘 기록/연속 기록/히스토리를 클라우드 백업 스냅샷으로 전체 덮어쓴다
    suspend fun restoreAll(today: DayRecord, streak: StreakInfo, history: Map<String, DayRecord>) {
        editSafely { prefs ->
            prefs[Keys.TODAY_RECORD] = json.encodeToString(today)
            prefs[Keys.STREAK_INFO] = json.encodeToString(streak)
            prefs[Keys.HISTORY] = json.encodeToString(history)
        }
    }
}
