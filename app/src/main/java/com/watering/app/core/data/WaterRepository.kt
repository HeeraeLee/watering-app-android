package com.watering.app.core.data

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

    suspend fun addEntry(amount: Int, drinkType: DrinkType, goal: Int): DayRecord =
        dataStore.addEntry(amount, drinkType, goal)

    suspend fun removeLastEntry(goal: Int): DayRecord =
        dataStore.removeLastEntry(goal)

    // 프리미엄 유저는 한 달에 하루, 정확히 하루를 놓쳤을 때만 연속 기록이 끊기지 않는다 (2일 이상 공백은 보호 대상 아님)
    suspend fun updateStreak(record: DayRecord, current: StreakInfo, isPremium: Boolean): StreakInfo {
        val todayKey = LocalDate.now().format(formatter)
        if (!record.isAchieved || record.dateKey != todayKey) return current

        val yesterdayKey = LocalDate.now().minusDays(1).format(formatter)
        val twoDaysAgoKey = LocalDate.now().minusDays(2).format(formatter)
        val currentMonthKey = LocalDate.now().format(monthFormatter)
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
}
