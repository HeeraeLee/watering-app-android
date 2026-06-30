package com.watering.app.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.watering.app.core.model.DrinkType
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first

class AddWaterAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )
        val waterRepository = entryPoint.waterRepository()
        val settingsRepository = entryPoint.settingsRepository()
        val waterService = entryPoint.waterService()

        val record = waterRepository.todayRecord.first()
        val settings = settingsRepository.userSettings.first()
        val currentRecord = record.copy(goal = settings.dailyGoal)

        val updated = waterService.addWater(
            amount = settings.cupSize,
            drinkType = DrinkType.WATER,
            currentRecord = currentRecord
        )
        waterService.updateStreak(updated, waterRepository.streakInfo.first())
        entryPoint.widgetUpdater().updateAll()
    }
}
