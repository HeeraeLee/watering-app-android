package com.watering.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.currentState
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

// Glance는 세션이 재사용될 때 provideGlance()를 다시 호출하지 않으므로,
// 갱신 시마다 이 키에 새 값을 써서 currentState() 변경 -> produceState 재실행을 유도한다.
val WidgetRefreshKey = longPreferencesKey("widget_refresh_token")

@Composable
fun rememberWidgetState(context: Context): WidgetState {
    val refreshToken = currentState(WidgetRefreshKey) ?: 0L
    val state by produceState(initialValue = WidgetState(), key1 = refreshToken) {
        value = loadWidgetState(context)
    }
    return state
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun waterRepository(): WaterRepository
    fun settingsRepository(): SettingsRepository
    fun waterService(): WaterService
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
