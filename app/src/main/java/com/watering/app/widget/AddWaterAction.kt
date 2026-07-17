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
            val analyticsService = entryPoint.analyticsService()
            val achievementChecker = entryPoint.achievementChecker()
            val achievementDataStore = entryPoint.achievementDataStore()

            val settings = settingsRepository.userSettings.first()
            // prev는 waterService.addWater() 내부의 DataStore 트랜잭션에서 원자적으로 캡처된 값을
            // 쓴다 — 별도로 .first()를 먼저 읽으면 위젯 연속 탭 시 그 사이에 다른 탭의 쓰기가 끼어들어
            // stale한 prev로 업적 경계를 잘못 판정할 수 있었음(WaterUpdateResult 참고)
            val result = waterService.addWater(
                amount = settings.cupSize,
                drinkType = DrinkType.WATER,
                goal = settings.dailyGoal,
                cupSize = settings.cupSize
            )
            val streak = waterService.updateStreak(result.updated, waterRepository.streakInfo.first())
            analyticsService.logRecordAdd(settings.cupSize, DrinkType.WATER.name, source = "widget")
            // 위젯에서 달성한 업적은 이 자리에서 모달을 못 띄우므로, 앱 재진입 시 HomeViewModel이
            // 보여줄 수 있도록 대기 상태로 저장해둔다 (AchievementDataStore.consumePendingDisplay 참고)
            achievementChecker.check(result.prev, result.updated, streak)?.let {
                achievementDataStore.setPendingDisplay(it)
            }
            Log.d("WateringWidget", "after: ${result.updated.totalCount}/${result.updated.goal}")
        } catch (e: Exception) {
            Log.e("WateringWidget", "AddWaterAction failed", e)
        }
    }
}
