package com.watering.app.widget

import android.content.Context
import android.util.Log
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
        Log.d("WateringWidget", "AddWaterAction triggered")
        try {
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
            Log.d("WateringWidget", "before: ${currentRecord.totalCount}/${currentRecord.goal}")

            val updated = waterService.addWater(
                amount = settings.cupSize,
                drinkType = DrinkType.WATER,
                currentRecord = currentRecord
            )
            waterService.updateStreak(updated, waterRepository.streakInfo.first())
            Log.d("WateringWidget", "after: ${updated.totalCount}/${updated.goal}")
        } catch (e: Exception) {
            Log.e("WateringWidget", "AddWaterAction failed", e)
        }
    }
}
