package com.watering.app.core.data

import com.watering.app.core.model.DailyAchievement
import com.watering.app.core.model.DayRecord
import com.watering.app.core.model.DrinkType
import com.watering.app.core.model.StreakInfo
import com.watering.app.core.datastore.WaterDataStore
import kotlinx.coroutines.flow.Flow
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

    suspend fun addEntry(amount: Int, drinkType: DrinkType, goal: Int): DayRecord =
        dataStore.addEntry(amount, drinkType, goal)

    suspend fun removeLastEntry(goal: Int): DayRecord =
        dataStore.removeLastEntry(goal)

    // 프리미엄 유저는 한 달에 하루, 정확히 하루를 놓쳤을 때만 연속 기록이 끊기지 않는다 (2일 이상 공백은 보호 대상 아님)
    suspend fun updateStreak(record: DayRecord, current: StreakInfo, isPremium: Boolean): StreakInfo {
        if (!record.isAchieved) return current

        // 시스템 시각(LocalDate.now())이 아닌 레코드 자체의 날짜를 기준으로 계산 — 자정 경계에서
        // 실제 현재 시각과 record.dateKey가 어긋나는 경우(코루틴 스케줄링 지연, 기기 슬립 등)에도
        // 정확하게 동작하도록 함 (AchievementChecker의 동일 버그, v0.24.1과 같은 수정 패턴)
        val todayKey = record.dateKey
        val today = LocalDate.parse(todayKey, formatter)
        val yesterdayKey = today.minusDays(1).format(formatter)
        val twoDaysAgoKey = today.minusDays(2).format(formatter)
        val currentMonthKey = today.format(monthFormatter)
        val protectionAvailable = isPremium &&
            (current.protectionUsedMonthKey != currentMonthKey || !current.protectionUsedThisMonth)

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

    suspend fun resetToday() = dataStore.resetTodayRecord()
    suspend fun clearAllData() = dataStore.clearAllData()

    suspend fun restoreAll(today: DayRecord, streak: StreakInfo, history: Map<String, DayRecord>) =
        dataStore.restoreAll(today, streak, history)
}
