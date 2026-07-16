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

    suspend fun addEntry(amount: Int, drinkType: DrinkType, goal: Int): WaterUpdateResult =
        dataStore.addEntry(amount, drinkType, goal)

    suspend fun removeLastEntry(goal: Int): DayRecord =
        dataStore.removeLastEntry(goal)

    // 한 달에 하루, 정확히 하루를 놓쳤을 때만 연속 기록이 끊기지 않는다 (2일 이상 공백은 보호 대상 아님)
    suspend fun updateStreak(record: DayRecord, current: StreakInfo): StreakInfo {
        if (!record.isAchieved) return current

        // 시스템 시각(LocalDate.now())이 아닌 레코드 자체의 날짜를 기준으로 계산 — 자정 경계에서
        // 실제 현재 시각과 record.dateKey가 어긋나는 경우(코루틴 스케줄링 지연, 기기 슬립 등)에도
        // 정확하게 동작하도록 함 (AchievementChecker의 동일 버그, v0.24.1과 같은 수정 패턴)
        val todayKey = record.dateKey
        val today = LocalDate.parse(todayKey, formatter)
        val yesterdayKey = today.minusDays(1).format(formatter)
        val twoDaysAgoKey = today.minusDays(2).format(formatter)
        val currentMonthKey = today.format(monthFormatter)
        val protectionAvailable =
            current.protectionUsedMonthKey != currentMonthKey || !current.protectionUsedThisMonth

        var protectionUsedThisMonth = current.protectionUsedThisMonth
        var protectionUsedMonthKey = current.protectionUsedMonthKey

        val newStreak = when {
            current.lastAchievedDateKey == yesterdayKey || current.currentStreak == 0 ->
                current.currentStreak + 1
            current.lastAchievedDateKey == todayKey ->
                current.currentStreak
            current.lastAchievedDateKey == twoDaysAgoKey && protectionAvailable -> {
                protectionUsedThisMonth = true
                protectionUsedMonthKey = currentMonthKey
                current.currentStreak + 1
            }
            else -> 1
        }
        val updated = current.copy(
            currentStreak = newStreak,
            longestStreak = maxOf(current.longestStreak, newStreak),
            lastAchievedDateKey = todayKey,
            protectionUsedThisMonth = protectionUsedThisMonth,
            protectionUsedMonthKey = protectionUsedMonthKey
        )
        dataStore.saveStreakInfo(updated)
        return updated
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

    suspend fun resetToday(goal: Int): DayRecord = dataStore.resetTodayRecord(goal)
    suspend fun clearAllData() = dataStore.clearAllData()

    suspend fun restoreAll(
        today: DayRecord,
        streak: StreakInfo,
        history: Map<String, DayRecord>,
        annualHistory: Map<String, DailyAchievement>
    ) = dataStore.restoreAll(today, streak, history, annualHistory)
}
