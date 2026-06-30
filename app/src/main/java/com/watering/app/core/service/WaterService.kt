package com.watering.app.core.service

import android.content.Context
import com.watering.app.core.data.WaterRepository
import com.watering.app.core.model.DayRecord
import com.watering.app.core.model.DrinkType
import com.watering.app.core.model.StreakInfo
import com.watering.app.widget.WateringWidgetUpdater
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WaterService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: WaterRepository,
    private val widgetUpdater: WateringWidgetUpdater
) {
    suspend fun addWater(
        amount: Int,
        drinkType: DrinkType = DrinkType.WATER,
        currentRecord: DayRecord
    ): DayRecord {
        val updated = repository.addEntry(amount, drinkType, currentRecord)
        widgetUpdater.updateAll()
        return updated
    }

    suspend fun undoLastEntry(currentRecord: DayRecord): DayRecord {
        val updated = repository.removeLastEntry(currentRecord)
        widgetUpdater.updateAll()
        return updated
    }

    suspend fun updateStreak(record: DayRecord, current: StreakInfo): StreakInfo =
        repository.updateStreak(record, current)

    suspend fun resetToday() {
        repository.resetToday()
        widgetUpdater.updateAll()
    }

    suspend fun clearAllData() {
        repository.clearAllData()
        widgetUpdater.updateAll()
    }
}
