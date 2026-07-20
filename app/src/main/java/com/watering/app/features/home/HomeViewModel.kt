package com.watering.app.features.home

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watering.app.R
import com.watering.app.core.data.SettingsRepository
import com.watering.app.core.data.WaterRepository
import com.watering.app.core.datastore.AchievementDataStore
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
    private val achievementDataStore: AchievementDataStore,
    private val reviewService: ReviewService,
    private val analyticsService: AnalyticsService
) : ViewModel() {

    private companion object {
        const val TAG = "HomeViewModel"
    }

    val uiState: StateFlow<HomeUiState> = combine(
        waterRepository.todayRecord,
        waterRepository.streakInfo,
        settingsRepository.userSettings
    ) { record, streak, settings ->
        HomeUiState(
            record = record.copy(goal = settings.dailyGoal, cupSize = settings.cupSize),
            streak = streak,
            settings = settings
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage

    private val _pendingAchievement = MutableStateFlow<Achievement?>(null)
    val pendingAchievement: StateFlow<Achievement?> = _pendingAchievement

    // 보호권이 방금 발동됐을 때만 잠깐 보여주는 배너 상태 (2026-07-20, UI 노출 시안 C 채택) —
    // streak.protectionUsedDates에 오늘 날짜가 새로 추가됐는지로 판정
    private val _showProtectionBanner = MutableStateFlow(false)
    val showProtectionBanner: StateFlow<Boolean> = _showProtectionBanner

    private fun checkProtectionTriggered(before: StreakInfo, after: StreakInfo, dateKey: String) {
        if (dateKey in after.protectionUsedDates && dateKey !in before.protectionUsedDates) {
            _showProtectionBanner.value = true
        }
    }

    fun dismissProtectionBanner() {
        _showProtectionBanner.value = false
    }

    init {
        // 위젯 탭으로 달성했지만 아직 못 보여준 업적이 있으면 앱 진입 시 모달로 보여준다
        viewModelScope.launch {
            achievementDataStore.consumePendingDisplay()?.let { _pendingAchievement.value = it }
        }
    }

    fun addWater(drinkType: DrinkType = DrinkType.WATER) {
        viewModelScope.launch {
            try {
                val current = uiState.value
                val result = waterService.addWater(
                    amount = current.settings.cupSize,
                    drinkType = drinkType,
                    goal = current.settings.dailyGoal,
                    cupSize = current.settings.cupSize
                )
                val streak = waterService.updateStreak(result.updated, current.streak)
                checkProtectionTriggered(current.streak, streak, result.updated.dateKey)
                _snackbarMessage.value = context.getString(R.string.home_snackbar_water_recorded, current.settings.cupSize)
                analyticsService.logRecordAdd(current.settings.cupSize, drinkType.name, source = "home")
                achievementChecker.check(result.prev, result.updated, streak)?.let { _pendingAchievement.value = it }
            } catch (e: Exception) {
                Log.w(TAG, "물 기록 추가 실패", e)
                _snackbarMessage.value = context.getString(R.string.home_snackbar_record_failed)
            }
        }
    }

    fun addWaterCustom(amount: Int, drinkType: DrinkType) {
        viewModelScope.launch {
            try {
                val current = uiState.value
                val result = waterService.addWater(
                    amount = amount,
                    drinkType = drinkType,
                    goal = current.settings.dailyGoal,
                    cupSize = current.settings.cupSize
                )
                val streak = waterService.updateStreak(result.updated, current.streak)
                checkProtectionTriggered(current.streak, streak, result.updated.dateKey)
                _snackbarMessage.value = context.getString(
                    R.string.home_snackbar_drink_recorded,
                    drinkType.emoji,
                    context.getString(drinkType.displayNameRes),
                    amount
                )
                analyticsService.logRecordAdd(amount, drinkType.name, source = "home")
                achievementChecker.check(result.prev, result.updated, streak)?.let { _pendingAchievement.value = it }
            } catch (e: Exception) {
                Log.w(TAG, "음료 기록 추가 실패", e)
                _snackbarMessage.value = context.getString(R.string.home_snackbar_record_failed)
            }
        }
    }

    fun dismissAchievement(activity: Activity? = null) {
        val achievement = _pendingAchievement.value
        _pendingAchievement.value = null
        if (achievement?.isStreakBased == true && activity != null) {
            viewModelScope.launch { reviewService.requestReviewIfEligible(activity) }
        }
    }

    fun onHomeScreenOpened(activity: Activity) {
        viewModelScope.launch { reviewService.requestReviewIfEligible(activity) }
    }

    fun undoLastEntry() {
        viewModelScope.launch {
            val current = uiState.value
            val updated = waterService.undoLastEntry(current.settings.dailyGoal, current.settings.cupSize)
            // uiState.value.streak(StateFlow 캐시)가 아니라 currentStreakInfo()로 직접 최신 값을
            // 읽는다 — 달성 직후 아주 빠르게 undo하면 StateFlow가 아직 재구독 전이라 옛 streak을
            // 롤백에 넘겨 방금 올라간 증가분이 안 지워질 수 있었음(2026-07-17, 조사 결과 ⑤)
            waterService.rollbackStreakAfterUndo(updated, waterService.currentStreakInfo())
            _snackbarMessage.value = null
            analyticsService.logRecordUndo()
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }
}
