package com.watering.app.core.data

import com.watering.app.core.model.DailyAchievement
import com.watering.app.core.model.DayRecord
import com.watering.app.core.model.DrinkType
import com.watering.app.core.model.StreakInfo
import com.watering.app.core.model.WaterUpdateResult
import com.watering.app.core.datastore.WaterDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WaterRepository @Inject constructor(
    private val dataStore: WaterDataStore
) {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

    val todayRecord: Flow<DayRecord> = dataStore.todayRecord
    val streakInfo: Flow<StreakInfo> = dataStore.streakInfo
    fun getHistory(): Flow<Map<String, DayRecord>> = dataStore.getHistory()
    fun getAnnualHistory(): Flow<Map<String, DailyAchievement>> = dataStore.getAnnualHistory()

    suspend fun addEntry(amount: Int, drinkType: DrinkType, goal: Int, cupSize: Int): WaterUpdateResult =
        dataStore.addEntry(amount, drinkType, goal, cupSize)

    suspend fun removeLastEntry(goal: Int, cupSize: Int): DayRecord =
        dataStore.removeLastEntry(goal, cupSize)

    // 한 달에 하루, 정확히 하루를 놓쳤을 때만 연속 기록이 끊기지 않는다 (2일 이상 공백은 보호 대상 아님)
    //
    // 순수 전이 계산은 advanceStreak()로 분리해뒀다 — recomputeStreakFromHistory()(백업 복원 시
    // 재계산)도 같은 함수를 재사용해 두 경로의 로직이 갈라지지 않게 하기 위함(2026-07-17).
    suspend fun updateStreak(record: DayRecord, current: StreakInfo): StreakInfo {
        if (!record.isAchieved) return current
        val updated = advanceStreak(record.dateKey, current)
        dataStore.saveStreakInfo(updated)
        return updated
    }

    // 시스템 시각(LocalDate.now())이 아닌 dateKey(달성한 날짜) 자체를 기준으로 계산 — 자정 경계에서
    // 실제 현재 시각과 어긋나는 경우(코루틴 스케줄링 지연, 기기 슬립 등)에도 정확하게 동작하도록 함
    // (AchievementChecker의 동일 버그, v0.24.1과 같은 수정 패턴)
    private fun advanceStreak(dateKey: String, current: StreakInfo): StreakInfo {
        val day = LocalDate.parse(dateKey, formatter)
        val yesterdayKey = day.minusDays(1).format(formatter)
        val twoDaysAgoKey = day.minusDays(2).format(formatter)
        val currentMonthKey = day.format(monthFormatter)
        val protectionAvailable =
            current.protectionUsedMonthKey != currentMonthKey || !current.protectionUsedThisMonth

        var protectionUsedThisMonth = current.protectionUsedThisMonth
        var protectionUsedMonthKey = current.protectionUsedMonthKey

        val newStreak = when {
            current.lastAchievedDateKey == yesterdayKey || current.currentStreak == 0 ->
                current.currentStreak + 1
            current.lastAchievedDateKey == dateKey ->
                current.currentStreak
            current.lastAchievedDateKey == twoDaysAgoKey && protectionAvailable -> {
                protectionUsedThisMonth = true
                protectionUsedMonthKey = currentMonthKey
                current.currentStreak + 1
            }
            else -> 1
        }
        return current.copy(
            currentStreak = newStreak,
            longestStreak = maxOf(current.longestStreak, newStreak),
            lastAchievedDateKey = dateKey,
            protectionUsedThisMonth = protectionUsedThisMonth,
            protectionUsedMonthKey = protectionUsedMonthKey
        )
    }

    // 백업 복원 시 저장된 StreakInfo 블롭을 그대로 믿지 않고, 같이 복원되는 annualHistory(365일
    // 집계)의 실제 달성 기록만 뽑아 처음부터 다시 재생(replay)해서 재계산한다 — 그렇지 않으면 연속
    // 기록이 최고치일 때 백업해두고 실제로는 스트릭이 끊긴 뒤 그 백업을 복원해 스트릭을 부당하게
    // 되살리거나, 보호권(월 1회) 소비 여부까지 초기화해 같은 달에 보호권을 여러 번 쓸 수 있는
    // 악용 경로가 있었다(2026-07-17, 목표 달성 악용 가능성 전수 조사 ④). annualHistory는 오늘 것이
    // 다음 기록 전까지 stale할 수 있어(SmartStatsViewModel과 동일 이유) todayRecord의 실시간
    // isAchieved로 오늘 항목만 덮어쓴다. annualHistory는 최근 365일만 보관하므로, 그보다 오래전에
    // 시작된 연속 기록/최장 기록은 이 재계산으로 완전히 복원되지 않을 수 있다는 한계가 있다(매우
    // 드문 경우로 판단해 감수함).
    fun recomputeStreakFromHistory(
        annualHistory: Map<String, DailyAchievement>,
        todayRecord: DayRecord
    ): StreakInfo {
        val achievedDateKeys = annualHistory
            .filterKeys { it != todayRecord.dateKey }
            .filterValues { it.isAchieved }
            .keys
            .toMutableSet()
        if (todayRecord.isAchieved) achievedDateKeys += todayRecord.dateKey
        return achievedDateKeys.sorted().fold(StreakInfo()) { acc, dateKey -> advanceStreak(dateKey, acc) }
    }

    // undo로 오늘 기록이 목표 미달성으로 바뀌었는데 그 달성으로 이미 streak이 갱신돼 있었다면,
    // updateStreak이 했던 증가를 정확히 되돌린다. lastAchievedDateKey가 오늘이 아니면(이 streak
    // 값이 애초에 오늘 달성으로 갱신된 게 아니면 — 예: 초과분만 취소해 여전히 달성 상태인 경우)
    // 아무 것도 하지 않는다.
    suspend fun rollbackStreakAfterUndo(record: DayRecord, current: StreakInfo): StreakInfo {
        if (record.isAchieved || current.lastAchievedDateKey != record.dateKey) return current

        val today = LocalDate.parse(record.dateKey, formatter)
        val yesterdayKey = today.minusDays(1).format(formatter)
        val twoDaysAgoKey = today.minusDays(2).format(formatter)
        val currentMonthKey = today.format(monthFormatter)
        val annualHistory = dataStore.getAnnualHistory().first()
        val yesterdayAchieved = annualHistory[yesterdayKey]?.isAchieved == true
        val twoDaysAgoAchieved = annualHistory[twoDaysAgoKey]?.isAchieved == true
        // updateStreak이 이틀 공백을 보호로 이어붙인 증가였는지 판별 — 그 경우에만 보호 사용
        // 플래그도 함께 되돌린다(다른 날짜에 쓴 보호까지 되돌리지 않도록)
        val usedProtectionToday = !yesterdayAchieved && twoDaysAgoAchieved &&
            current.protectionUsedThisMonth && current.protectionUsedMonthKey == currentMonthKey

        val updated = current.copy(
            currentStreak = (current.currentStreak - 1).coerceAtLeast(0),
            // 이번 갱신이 막 최장 기록을 세운 것이었을 때만 최장 기록도 함께 되돌림 — 이전 최장
            // 기록과 우연히 값이 같았던 경우까지는 구분할 수 없는 한계가 있음
            longestStreak = if (current.longestStreak == current.currentStreak) {
                (current.longestStreak - 1).coerceAtLeast(0)
            } else current.longestStreak,
            lastAchievedDateKey = when {
                yesterdayAchieved -> yesterdayKey
                usedProtectionToday -> twoDaysAgoKey
                else -> current.lastAchievedDateKey
            },
            protectionUsedThisMonth = if (usedProtectionToday) false else current.protectionUsedThisMonth
        )
        dataStore.saveStreakInfo(updated)
        return updated
    }

    suspend fun resetToday(goal: Int, cupSize: Int): DayRecord = dataStore.resetTodayRecord(goal, cupSize)

    suspend fun resetTodayIfStale(goal: Int, cupSize: Int): DayRecord? =
        dataStore.resetTodayRecordIfStale(goal, cupSize)

    suspend fun clearAllData() = dataStore.clearAllData()

    // streak(백업된 StreakInfo 원본)은 그대로 신뢰하지 않는다 — annualHistory 기준으로 재계산한
    // 값(recomputeStreakFromHistory)을 대신 저장한다. 파라미터 자체는 호출부(BackupService)가
    // 백업 페이로드를 그대로 넘기는 기존 인터페이스를 유지하기 위해 남겨둠(2026-07-17).
    suspend fun restoreAll(
        today: DayRecord,
        streak: StreakInfo,
        history: Map<String, DayRecord>,
        annualHistory: Map<String, DailyAchievement>
    ) = dataStore.restoreAll(today, recomputeStreakFromHistory(annualHistory, today), history, annualHistory)
}
