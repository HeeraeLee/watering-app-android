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

    val todayRecord: Flow<DayRecord> = dataStore.todayRecord
    val streakInfo: Flow<StreakInfo> = dataStore.streakInfo
    fun getHistory(): Flow<Map<String, DayRecord>> = dataStore.getHistory()

    suspend fun addEntry(amount: Int, drinkType: DrinkType, goal: Int): DayRecord =
        dataStore.addEntry(amount, drinkType, goal)

    suspend fun removeLastEntry(goal: Int): DayRecord =
        dataStore.removeLastEntry(goal)

    suspend fun updateStreak(record: DayRecord, current: StreakInfo): StreakInfo {
        val todayKey = LocalDate.now().format(formatter)
        if (!record.isAchieved || record.dateKey != todayKey) return current

        val yesterdayKey = LocalDate.now().minusDays(1).format(formatter)
        val newStreak = if (current.lastAchievedDateKey == yesterdayKey || current.currentStreak == 0) {
            current.currentStreak + 1
        } else if (current.lastAchievedDateKey == todayKey) {
            current.currentStreak
        } else {
            1
        }
        val updated = current.copy(
            currentStreak = newStreak,
            longestStreak = maxOf(current.longestStreak, newStreak),
            lastAchievedDateKey = todayKey
        )
        dataStore.saveStreakInfo(updated)
        return updated
    }

    suspend fun resetToday() = dataStore.resetTodayRecord()
    suspend fun clearAllData() = dataStore.clearAllData()
}
