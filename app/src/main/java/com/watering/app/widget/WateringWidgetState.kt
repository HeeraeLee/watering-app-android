package com.watering.app.widget

import android.content.Context
import com.watering.app.core.data.SettingsRepository
import com.watering.app.core.data.WaterRepository
import com.watering.app.core.service.WaterService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

data class WidgetState(
    val totalCount: Int = 0,
    val goal: Int = 8,
    val achievementRate: Double = 0.0
)

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun waterRepository(): WaterRepository
    fun settingsRepository(): SettingsRepository
    fun waterService(): WaterService
    fun widgetUpdater(): WateringWidgetUpdater
}

suspend fun loadWidgetState(context: Context): WidgetState {
    return try {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )
        val record = entryPoint.waterRepository().todayRecord.first()
        val settings = entryPoint.settingsRepository().userSettings.first()
        val goal = settings.dailyGoal.coerceAtLeast(1)
        WidgetState(
            totalCount = record.totalCount,
            goal = goal,
            achievementRate = record.totalCount.toDouble() / goal
        )
    } catch (e: Exception) {
        WidgetState()
    }
}
