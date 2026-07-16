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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface WeightGoalUiState {
    data object Idle : WeightGoalUiState
    data class Editing(val weightInput: String, val recommendedCups: Int?) : WeightGoalUiState
}

// 컵 크기 변경 확인 다이얼로그 상태 — entries.size는 기록 당시 ml과 무관하게 개수만 세서, 컵
// 크기를 바꾸면 이미 기록한 항목이 새 크기 기준으로 섞여 보이는 문제가 있어(2026-07-16, owner
// 제보) 오늘 기록이 있을 때는 바로 적용하지 않고 사용자에게 초기화/유지를 먼저 물어본다.
sealed interface CupSizeChangeUiState {
    data object Idle : CupSizeChangeUiState
    data class Confirming(val newSize: Int, val todayTotalMl: Int, val todayCount: Int) : CupSizeChangeUiState
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

    private val _cupSizeChangeUiState = MutableStateFlow<CupSizeChangeUiState>(CupSizeChangeUiState.Idle)
    val cupSizeChangeUiState: StateFlow<CupSizeChangeUiState> = _cupSizeChangeUiState.asStateFlow()

    // 컵 크기 변경 요청 — 오늘 이미 기록이 있으면 확인 다이얼로그를 먼저 띄운다(2026-07-16, owner
    // 제보: entries.size는 기록 당시 ml과 무관하게 개수만 세서, 컵 크기를 바꾸면 이미 기록한 항목이
    // 새 크기 기준으로 섞여 보이는 문제가 있었음). 기록이 없거나 크기가 실제로 안 바뀌면 바로 적용.
    fun requestCupSizeChange(newSize: Int) {
        if (newSize == settings.value.cupSize) return
        viewModelScope.launch {
            val todayEntries = waterService.currentTodayRecord().entries
            if (todayEntries.isEmpty()) {
                applyCupSizeChange(newSize, resetTodayFirst = false)
            } else {
                _cupSizeChangeUiState.value = CupSizeChangeUiState.Confirming(
                    newSize = newSize,
                    todayTotalMl = todayEntries.sumOf { it.amount },
                    todayCount = todayEntries.size
                )
            }
        }
    }

    fun confirmCupSizeChange(resetToday: Boolean) {
        val state = _cupSizeChangeUiState.value as? CupSizeChangeUiState.Confirming ?: return
        _cupSizeChangeUiState.value = CupSizeChangeUiState.Idle
        viewModelScope.launch { applyCupSizeChange(state.newSize, resetTodayFirst = resetToday) }
    }

    fun dismissCupSizeChangeDialog() {
        _cupSizeChangeUiState.value = CupSizeChangeUiState.Idle
    }

    // 몸무게 기반 목표가 적용된 상태(weightKg 존재)라면 컵 크기가 바뀌어도 목표 수분량(ml)이
    // 유지되도록 잔 수를 다시 계산하되, 오늘 이미 마신 양(초기화했다면 0)을 반영해 진행률이
    // 역전되지 않게 한다(recommendedGoalCupsPreservingProgress). 그렇지 않으면 기존 잔 수 그대로
    // 컵 크기만 바뀐다.
    private suspend fun applyCupSizeChange(newSize: Int, resetTodayFirst: Boolean) {
        if (resetTodayFirst) waterService.resetToday()
        val weightKg = settings.value.weightKg
        if (weightKg == null) {
            update { it.copy(cupSize = newSize) }
            return
        }
        val todayEntries = waterService.currentTodayRecord().entries
        val newGoal = StatsInsightService.recommendedGoalCupsPreservingProgress(weightKg, newSize, todayEntries)
        update(refreshWidget = true) { it.copy(cupSize = newSize, dailyGoal = newGoal) }
        _snackbarMessage.value = context.getString(R.string.settings_cup_size_goal_adjusted_snackbar, newGoal)
    }

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

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    // 저장된 몸무게가 있으면 "마지막 입력: OOkg → 하루 N잔"으로, 없으면 기본 안내문을 보여준다
    val weightGoalSubtitle: StateFlow<String> = settings
        .map { s ->
            val weightKg = s.weightKg
            if (weightKg == null) {
                context.getString(R.string.settings_weight_goal_subtitle)
            } else {
                val cups = StatsInsightService.recommendedGoalCups(weightKg, s.cupSize)
                context.getString(R.string.settings_weight_goal_last_input, formatWeightInput(weightKg), cups)
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            context.getString(R.string.settings_weight_goal_subtitle)
        )

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
        _snackbarMessage.value = context.getString(
            R.string.settings_weight_goal_applied_snackbar,
            formatWeightInput(weightKg),
            cups
        )
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
            val previous = settings.value
            val updated = transform(previous)
            settingsRepository.updateSettings(updated)
            if (refreshWidget) widgetUpdater.updateAll()
            if (updated.notificationEnabled) {
                notificationService.scheduleReminders(updated)
            } else {
                notificationService.cancelReminders()
            }
            // 목표가 바뀐 경우에만 동기화 — updateDailyGoal/applyCupSizeChange/applyWeightGoal
            // 세 경로 모두 여기로 모이므로 한 곳에서 처리(오늘 즉시 달성으로 바뀌었는데 streak이
            // 안 따라오던 모순 수정)
            if (updated.dailyGoal != previous.dailyGoal) {
                waterService.syncStreakForGoalChange(updated.dailyGoal)
            }
        }
    }
}
