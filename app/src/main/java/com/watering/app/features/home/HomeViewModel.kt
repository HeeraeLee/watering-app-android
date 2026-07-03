package com.watering.app.features.home

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watering.app.R
import com.watering.app.core.data.SettingsRepository
import com.watering.app.core.data.WaterRepository
import com.watering.app.core.model.Achievement
import com.watering.app.core.model.DayRecord
import com.watering.app.core.model.DrinkType
import com.watering.app.core.model.StreakInfo
import com.watering.app.core.model.UserSettings
import com.watering.app.core.service.AchievementChecker
import com.watering.app.core.service.AnalyticsService
import com.watering.app.core.service.ReviewService
import com.watering.app.core.service.WaterService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context,
    private val waterService: WaterService,
    private val waterRepository: WaterRepository,
    private val settingsRepository: SettingsRepository,
    private val achievementChecker: AchievementChecker,
    private val reviewService: ReviewService,
    private val analyticsService: AnalyticsService
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

    private val _pendingAchievement = MutableStateFlow<Achievement?>(null)
    val pendingAchievement: StateFlow<Achievement?> = _pendingAchievement

    fun addWater(drinkType: DrinkType = DrinkType.WATER) {
        viewModelScope.launch {
            val current = uiState.value
            val prev = current.record
            val updated = waterService.addWater(
                amount = current.settings.cupSize,
                drinkType = drinkType,
                goal = current.settings.dailyGoal
            )
            val streak = waterService.updateStreak(updated, current.streak, current.settings.isPremium)
            _snackbarMessage.value = context.getString(R.string.home_snackbar_water_recorded, current.settings.cupSize)
            analyticsService.logRecordAdd(current.settings.cupSize, drinkType.name, source = "home")
            achievementChecker.check(prev, updated, streak)?.let { _pendingAchievement.value = it }
        }
    }

    fun addWaterCustom(amount: Int, drinkType: DrinkType) {
        viewModelScope.launch {
            val current = uiState.value
            val prev = current.record
            val updated = waterService.addWater(
                amount = amount,
                drinkType = drinkType,
                goal = current.settings.dailyGoal
            )
            val streak = waterService.updateStreak(updated, current.streak, current.settings.isPremium)
            _snackbarMessage.value = context.getString(
                R.string.home_snackbar_drink_recorded,
                drinkType.emoji,
                context.getString(drinkType.displayNameRes),
                amount
            )
            analyticsService.logRecordAdd(amount, drinkType.name, source = "home")
            achievementChecker.check(prev, updated, streak)?.let { _pendingAchievement.value = it }
        }
    }

    fun dismissAchievement(activity: Activity? = null) {
        val achievement = _pendingAchievement.value
        _pendingAchievement.value = null
        if (achievement == Achievement.STREAK_7 && activity != null) {
            viewModelScope.launch { reviewService.requestReviewIfEligible(activity) }
        }
    }

    fun undoLastEntry() {
        viewModelScope.launch {
            waterService.undoLastEntry(uiState.value.settings.dailyGoal)
            _snackbarMessage.value = null
            analyticsService.logRecordUndo()
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }
}
