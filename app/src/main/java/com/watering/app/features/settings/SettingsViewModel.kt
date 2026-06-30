package com.watering.app.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watering.app.core.data.SettingsRepository
import com.watering.app.core.model.UserSettings
import com.watering.app.core.service.NotificationService
import com.watering.app.core.service.WaterService
import com.watering.app.widget.WateringWidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val notificationService: NotificationService,
    private val waterService: WaterService,
    private val widgetUpdater: WateringWidgetUpdater
) : ViewModel() {

    val settings: StateFlow<UserSettings> = settingsRepository.userSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    fun updateDailyGoal(goal: Int) = update { it.copy(dailyGoal = goal) }
    fun updateCupSize(size: Int) = update { it.copy(cupSize = size) }
    fun updateNotificationEnabled(enabled: Boolean) = update { it.copy(notificationEnabled = enabled) }
    fun updateNotificationInterval(minutes: Int) = update { it.copy(notificationInterval = minutes) }
    fun updateNotificationStart(hour: Int) = update { it.copy(notificationStart = hour) }
    fun updateNotificationEnd(hour: Int) = update { it.copy(notificationEnd = hour) }
    fun updateHealthConnect(enabled: Boolean) = update { it.copy(healthConnectEnabled = enabled) }

    fun resetAllData() {
        viewModelScope.launch { waterService.clearAllData() }
    }

    private fun update(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch {
            val updated = transform(settings.value)
            settingsRepository.updateSettings(updated)
            widgetUpdater.updateAll()
            if (updated.notificationEnabled) {
                notificationService.scheduleReminders(updated)
            } else {
                notificationService.cancelReminders()
            }
        }
    }
}
