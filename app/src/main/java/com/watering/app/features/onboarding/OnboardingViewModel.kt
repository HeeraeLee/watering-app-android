package com.watering.app.features.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watering.app.core.data.SettingsRepository
import com.watering.app.core.model.UserSettings
import com.watering.app.core.service.NotificationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val notificationService: NotificationService
) : ViewModel() {

    val isOnboardingDone: StateFlow<Boolean?> = settingsRepository.userSettings
        .map { it.isOnboardingDone }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun completeOnboarding(dailyGoal: Int, cupSize: Int, notificationEnabled: Boolean) {
        viewModelScope.launch {
            val settings = UserSettings(
                dailyGoal = dailyGoal,
                cupSize = cupSize,
                notificationEnabled = notificationEnabled,
                isOnboardingDone = true
            )
            settingsRepository.updateSettings(settings)
            if (notificationEnabled) {
                notificationService.scheduleReminders(settings)
            }
        }
    }
}
