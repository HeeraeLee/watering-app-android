package com.watering.app.features.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watering.app.core.data.SettingsRepository
import com.watering.app.core.model.UserSettings
import com.watering.app.core.model.WidgetTheme
import com.watering.app.core.service.HealthConnectAvailability
import com.watering.app.core.service.HealthConnectService
import com.watering.app.core.service.NotificationService
import com.watering.app.core.service.StatsInsightService
import com.watering.app.core.service.WaterService
import com.watering.app.widget.WateringWidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface WeightGoalUiState {
    data object Idle : WeightGoalUiState
    data object Loading : WeightGoalUiState
    data object NeedsPermission : WeightGoalUiState
    data object PermissionDenied : WeightGoalUiState
    data class NotAvailable(val availability: HealthConnectAvailability) : WeightGoalUiState
    data object NoWeightData : WeightGoalUiState
    data class Recommended(val cups: Int, val weightKg: Double) : WeightGoalUiState
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val notificationService: NotificationService,
    private val waterService: WaterService,
    private val widgetUpdater: WateringWidgetUpdater,
    private val healthConnectService: HealthConnectService
) : ViewModel() {

    val settings: StateFlow<UserSettings> = settingsRepository.userSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    private val _notificationPermissionGranted = MutableStateFlow(hasNotificationPermission())
    val notificationPermissionGranted: StateFlow<Boolean> = _notificationPermissionGranted.asStateFlow()

    // 시스템 설정 화면에서 권한을 바꾸고 돌아올 수 있으므로 화면 재진입 시마다 재확인 필요
    fun refreshNotificationPermission() {
        _notificationPermissionGranted.value = hasNotificationPermission()
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun updateDailyGoal(goal: Int) = update(refreshWidget = true) { it.copy(dailyGoal = goal) }
    fun updateCupSize(size: Int) = update { it.copy(cupSize = size) }
    fun updateNotificationEnabled(enabled: Boolean) = update { it.copy(notificationEnabled = enabled) }
    fun updateNotificationInterval(minutes: Int) = update { it.copy(notificationInterval = minutes) }
    fun updateNotificationStart(hour: Int) = update { it.copy(notificationStart = hour) }
    fun updateNotificationEnd(hour: Int) = update { it.copy(notificationEnd = hour) }
    fun updateWidgetTheme(theme: WidgetTheme) = update(refreshWidget = true) { it.copy(widgetTheme = theme) }

    fun resetAllData() {
        viewModelScope.launch { waterService.clearAllData() }
    }

    private val _weightGoalUiState = MutableStateFlow<WeightGoalUiState>(WeightGoalUiState.Idle)
    val weightGoalUiState: StateFlow<WeightGoalUiState> = _weightGoalUiState.asStateFlow()

    fun healthConnectPermissionContract() = healthConnectService.permissionRequestContract()

    fun requestWeightBasedGoal() {
        viewModelScope.launch {
            _weightGoalUiState.value = WeightGoalUiState.Loading
            val availability = healthConnectService.availability
            if (availability != HealthConnectAvailability.AVAILABLE) {
                _weightGoalUiState.value = WeightGoalUiState.NotAvailable(availability)
                return@launch
            }
            if (!healthConnectService.hasPermissions(HealthConnectService.WEIGHT_PERMISSIONS)) {
                _weightGoalUiState.value = WeightGoalUiState.NeedsPermission
                return@launch
            }
            readWeightAndRecommend()
        }
    }

    fun onWeightPermissionResult(granted: Set<String>) {
        if (!granted.containsAll(HealthConnectService.WEIGHT_PERMISSIONS)) {
            _weightGoalUiState.value = WeightGoalUiState.PermissionDenied
            return
        }
        viewModelScope.launch {
            _weightGoalUiState.value = WeightGoalUiState.Loading
            readWeightAndRecommend()
        }
    }

    private suspend fun readWeightAndRecommend() {
        val weightKg = healthConnectService.readLatestWeightKg()
        _weightGoalUiState.value = if (weightKg == null) {
            WeightGoalUiState.NoWeightData
        } else {
            val cups = StatsInsightService.recommendedGoalCups(weightKg, settings.value.cupSize)
            WeightGoalUiState.Recommended(cups, weightKg)
        }
    }

    fun applyRecommendedGoal(cups: Int) {
        updateDailyGoal(cups)
        _weightGoalUiState.value = WeightGoalUiState.Idle
    }

    fun dismissWeightGoalState() {
        _weightGoalUiState.value = WeightGoalUiState.Idle
    }

    private fun update(refreshWidget: Boolean = false, transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch {
            val updated = transform(settings.value)
            settingsRepository.updateSettings(updated)
            if (refreshWidget) widgetUpdater.updateAll()
            if (updated.notificationEnabled) {
                notificationService.scheduleReminders(updated)
            } else {
                notificationService.cancelReminders()
            }
        }
    }
}
