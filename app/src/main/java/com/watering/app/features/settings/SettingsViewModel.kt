package com.watering.app.features.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watering.app.R
import com.watering.app.core.data.SettingsRepository
import com.watering.app.core.model.UserSettings
import com.watering.app.core.model.WidgetTheme
import com.watering.app.core.service.CsvExportService
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
    data class Editing(val weightInput: String, val recommendedCups: Int?) : WeightGoalUiState
}

sealed interface HydrationSyncUiState {
    data object Idle : HydrationSyncUiState
    data object NeedsPermission : HydrationSyncUiState
    data class NotAvailable(val availability: HealthConnectAvailability) : HydrationSyncUiState
    data object PermissionDenied : HydrationSyncUiState
}

sealed interface CsvExportUiState {
    data object Idle : CsvExportUiState
    data object Loading : CsvExportUiState
    data class Success(val uri: Uri) : CsvExportUiState
    data class Error(val message: String) : CsvExportUiState
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val notificationService: NotificationService,
    private val waterService: WaterService,
    private val widgetUpdater: WateringWidgetUpdater,
    private val healthConnectService: HealthConnectService,
    private val csvExportService: CsvExportService
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

    // 디버그 빌드 전용 — 실제 결제 없이 프리미엄 기능을 테스트하기 위한 토글 (release 빌드에는 노출 안 됨)
    fun toggleDebugPremium() = update(refreshWidget = true) { it.copy(isPremium = !it.isPremium) }

    private val _weightGoalUiState = MutableStateFlow<WeightGoalUiState>(WeightGoalUiState.Idle)
    val weightGoalUiState: StateFlow<WeightGoalUiState> = _weightGoalUiState.asStateFlow()

    fun healthConnectPermissionContract() = healthConnectService.permissionRequestContract()

    // 저장된 몸무게가 있으면 입력값을 미리 채워서 열고, 없으면 빈 입력으로 연다
    fun openWeightGoalDialog() {
        val savedWeightKg = settings.value.weightKg
        _weightGoalUiState.value = WeightGoalUiState.Editing(
            weightInput = savedWeightKg?.let { formatWeightInput(it) } ?: "",
            recommendedCups = savedWeightKg?.let {
                StatsInsightService.recommendedGoalCups(it, settings.value.cupSize)
            }
        )
    }

    fun onWeightInputChange(input: String) {
        val weightKg = input.toDoubleOrNull()
        val cups = if (weightKg != null && weightKg > 0) {
            StatsInsightService.recommendedGoalCups(weightKg, settings.value.cupSize)
        } else {
            null
        }
        _weightGoalUiState.value = WeightGoalUiState.Editing(input, cups)
    }

    fun applyWeightGoal() {
        val state = _weightGoalUiState.value
        if (state !is WeightGoalUiState.Editing) return
        val weightKg = state.weightInput.toDoubleOrNull() ?: return
        val cups = state.recommendedCups ?: return
        update(refreshWidget = true) { it.copy(weightKg = weightKg, dailyGoal = cups) }
        _weightGoalUiState.value = WeightGoalUiState.Idle
    }

    fun dismissWeightGoalState() {
        _weightGoalUiState.value = WeightGoalUiState.Idle
    }

    private fun formatWeightInput(weightKg: Double): String =
        if (weightKg == weightKg.toLong().toDouble()) weightKg.toLong().toString() else weightKg.toString()

    private val _hydrationSyncUiState = MutableStateFlow<HydrationSyncUiState>(HydrationSyncUiState.Idle)
    val hydrationSyncUiState: StateFlow<HydrationSyncUiState> = _hydrationSyncUiState.asStateFlow()

    fun onHydrationSyncToggle(enabled: Boolean) {
        if (!enabled) {
            update { it.copy(healthConnectEnabled = false) }
            return
        }
        viewModelScope.launch {
            val availability = healthConnectService.availability
            if (availability != HealthConnectAvailability.AVAILABLE) {
                _hydrationSyncUiState.value = HydrationSyncUiState.NotAvailable(availability)
                return@launch
            }
            if (healthConnectService.hasPermissions(HealthConnectService.HYDRATION_PERMISSIONS)) {
                update { it.copy(healthConnectEnabled = true) }
            } else {
                _hydrationSyncUiState.value = HydrationSyncUiState.NeedsPermission
            }
        }
    }

    fun onHydrationPermissionResult(granted: Set<String>) {
        if (granted.containsAll(HealthConnectService.HYDRATION_PERMISSIONS)) {
            update { it.copy(healthConnectEnabled = true) }
        } else {
            _hydrationSyncUiState.value = HydrationSyncUiState.PermissionDenied
        }
    }

    fun dismissHydrationSyncState() {
        _hydrationSyncUiState.value = HydrationSyncUiState.Idle
    }

    private val _csvExportUiState = MutableStateFlow<CsvExportUiState>(CsvExportUiState.Idle)
    val csvExportUiState: StateFlow<CsvExportUiState> = _csvExportUiState.asStateFlow()

    fun exportCsv() {
        viewModelScope.launch {
            _csvExportUiState.value = CsvExportUiState.Loading
            csvExportService.exportToCsv()
                .onSuccess { _csvExportUiState.value = CsvExportUiState.Success(it) }
                .onFailure { _csvExportUiState.value = CsvExportUiState.Error(context.getString(R.string.settings_csv_export_error)) }
        }
    }

    fun dismissCsvExportState() {
        _csvExportUiState.value = CsvExportUiState.Idle
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
