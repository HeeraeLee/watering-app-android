package com.watering.app.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watering.app.core.data.SettingsRepository
import com.watering.app.core.data.WaterRepository
import com.watering.app.core.model.DayRecord
import com.watering.app.core.model.DrinkType
import com.watering.app.core.model.StreakInfo
import com.watering.app.core.model.UserSettings
import com.watering.app.core.service.WaterService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val record: DayRecord = DayRecord(dateKey = ""),
    val streak: StreakInfo = StreakInfo(),
    val settings: UserSettings = UserSettings(),
    val showUndoSnackbar: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val waterService: WaterService,
    private val waterRepository: WaterRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        waterRepository.todayRecord,
        waterRepository.streakInfo,
        settingsRepository.userSettings
    ) { record, streak, settings ->
        HomeUiState(
            record = record.copy(goal = settings.dailyGoal),
            streak = streak,
            settings = settings
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage

    fun addWater(drinkType: DrinkType = DrinkType.WATER) {
        viewModelScope.launch {
            val current = uiState.value
            val record = current.record.copy(goal = current.settings.dailyGoal)
            val updated = waterService.addWater(
                amount = current.settings.cupSize,
                drinkType = drinkType,
                currentRecord = record
            )
            waterService.updateStreak(updated, current.streak)
            _snackbarMessage.value = "💧 +${current.settings.cupSize}ml 기록됐어요"
        }
    }

    fun addWaterCustom(amount: Int, drinkType: DrinkType) {
        viewModelScope.launch {
            val current = uiState.value
            val record = current.record.copy(goal = current.settings.dailyGoal)
            val updated = waterService.addWater(
                amount = amount,
                drinkType = drinkType,
                currentRecord = record
            )
            waterService.updateStreak(updated, current.streak)
            _snackbarMessage.value = "${drinkType.emoji} ${drinkType.displayName} +${amount}ml 기록됐어요"
        }
    }

    fun undoLastEntry() {
        viewModelScope.launch {
            waterService.undoLastEntry(uiState.value.record)
            _snackbarMessage.value = null
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }
}
