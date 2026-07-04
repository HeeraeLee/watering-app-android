package com.watering.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.currentState
import com.watering.app.core.data.SettingsRepository
import com.watering.app.core.data.WaterRepository
import com.watering.app.core.model.WidgetTheme
import com.watering.app.core.service.AchievementChecker
import com.watering.app.core.service.AnalyticsService
import com.watering.app.core.service.WaterService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

data class WidgetState(
    val totalCount: Int = 0,
    val goal: Int = 8,
    val achievementRate: Double = 0.0,
    val theme: WidgetTheme = WidgetTheme.DEFAULT
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
    fun analyticsService(): AnalyticsService
    fun achievementChecker(): AchievementChecker
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
        // 구독이 만료돼도 선택했던 테마는 저장된 채로 두고 위젯만 기본 테마로 되돌린다 (재구독 시 즉시 복원)
        val theme = if (settings.isPremium) settings.widgetTheme else WidgetTheme.DEFAULT
        WidgetState(
            totalCount = record.totalCount,
            goal = goal,
            achievementRate = record.totalCount.toDouble() / goal,
            theme = theme
        )
    } catch (e: Exception) {
        WidgetState()
    }
}
