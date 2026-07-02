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
import com.watering.app.core.service.NotificationService
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

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val notificationService: NotificationService,
    private val waterService: WaterService,
    private val widgetUpdater: WateringWidgetUpdater
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
    fun updateHealthConnect(enabled: Boolean) = update { it.copy(healthConnectEnabled = enabled) }

    fun resetAllData() {
        viewModelScope.launch { waterService.clearAllData() }
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
