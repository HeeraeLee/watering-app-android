package com.watering.app.features.home

import android.app.Activity
import android.content.Context
import app.cash.turbine.test
import com.watering.app.R
import com.watering.app.core.data.SettingsRepository
import com.watering.app.core.data.WaterRepository
import com.watering.app.core.datastore.AchievementDataStore
import com.watering.app.core.model.Achievement
import com.watering.app.core.model.DayRecord
import com.watering.app.core.model.DrinkType
import com.watering.app.core.model.StreakInfo
import com.watering.app.core.model.UserSettings
import com.watering.app.core.model.WaterUpdateResult
import com.watering.app.core.service.AchievementChecker
import com.watering.app.core.service.AnalyticsService
import com.watering.app.core.service.ReviewService
import com.watering.app.core.service.WaterService
import com.watering.app.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var context: Context
    private lateinit var waterService: WaterService
    private lateinit var waterRepository: WaterRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var achievementChecker: AchievementChecker
    private lateinit var achievementDataStore: AchievementDataStore
    private lateinit var reviewService: ReviewService
    private lateinit var analyticsService: AnalyticsService

    private val record = DayRecord(dateKey = "2026-07-02", goal = 8)
    private val streak = StreakInfo(currentStreak = 2)
    private val settings = UserSettings(dailyGoal = 8, cupSize = 200)

    private fun createViewModel(pendingFromWidget: Achievement? = null): HomeViewModel {
        context = mockk {
            every { getString(R.string.home_snackbar_water_recorded, 200) } returns "💧 +200ml 기록됐어요"
            every { getString(DrinkType.COFFEE.displayNameRes) } returns "커피"
            every {
                getString(R.string.home_snackbar_drink_recorded, "☕", "커피", 350)
            } returns "☕ 커피 +350ml 기록됐어요"
        }
        waterService = mockk(relaxed = true)
        waterRepository = mockk {
            every { todayRecord } returns MutableStateFlow(record)
            every { streakInfo } returns MutableStateFlow(streak)
        }
        settingsRepository = mockk {
            every { userSettings } returns MutableStateFlow(settings)
        }
        achievementChecker = mockk()
        achievementDataStore = mockk {
            coEvery { consumePendingDisplay() } returns pendingFromWidget
        }
        reviewService = mockk(relaxed = true)
        analyticsService = mockk(relaxed = true)
        return HomeViewModel(context, waterService, waterRepository, settingsRepository, achievementChecker, achievementDataStore, reviewService, analyticsService)
    }

    @Test
    fun uiState_세flow를조합해record의goal을settings기준으로덮어쓴다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(8, state.record.goal)
            assertEquals(streak, state.streak)
            assertEquals(settings, state.settings)
        }
    }

    @Test
    fun addWater_기본물마시기는cupSize만큼기록하고스낵바메시지를설정한다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        val updated = record.copy(entries = record.entries)
        coEvery { waterService.addWater(200, DrinkType.WATER, 8, 200) } returns WaterUpdateResult(record, updated)
        coEvery { waterService.updateStreak(updated, streak) } returns streak
        coEvery { achievementChecker.check(any(), updated, streak) } returns null

        viewModel.uiState.test {
            awaitItem()
            viewModel.addWater()
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals("💧 +200ml 기록됐어요", viewModel.snackbarMessage.value)
        assertNull(viewModel.pendingAchievement.value)
        coVerify { waterService.addWater(200, DrinkType.WATER, 8, 200) }
    }

    @Test
    fun addWater_업적을달성하면pendingAchievement가설정된다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        val updated = record.copy()
        coEvery { waterService.addWater(200, DrinkType.WATER, 8, 200) } returns WaterUpdateResult(record, updated)
        coEvery { waterService.updateStreak(updated, streak) } returns streak
        coEvery { achievementChecker.check(any(), updated, streak) } returns Achievement.GOAL_ACHIEVED

        viewModel.uiState.test {
            awaitItem()
            viewModel.addWater()
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(Achievement.GOAL_ACHIEVED, viewModel.pendingAchievement.value)
    }

    @Test
    fun init_위젯에서달성한pending업적이있으면pendingAchievement로로드한다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel(pendingFromWidget = Achievement.STREAK_7)

        assertEquals(Achievement.STREAK_7, viewModel.pendingAchievement.value)
        coVerify { achievementDataStore.consumePendingDisplay() }
    }

    @Test
    fun init_대기중인업적이없으면pendingAchievement는null이다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel(pendingFromWidget = null)

        assertNull(viewModel.pendingAchievement.value)
    }

    @Test
    fun addWaterCustom_지정한음료와용량으로기록하고메시지를설정한다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        val updated = record.copy()
        coEvery { waterService.addWater(350, DrinkType.COFFEE, 8, 200) } returns WaterUpdateResult(record, updated)
        coEvery { waterService.updateStreak(updated, streak) } returns streak
        coEvery { achievementChecker.check(any(), updated, streak) } returns null

        viewModel.uiState.test {
            awaitItem()
            viewModel.addWaterCustom(amount = 350, drinkType = DrinkType.COFFEE)
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals("☕ 커피 +350ml 기록됐어요", viewModel.snackbarMessage.value)
    }

    @Test
    fun dismissAchievement_연속기록업적이고activity가있으면리뷰요청한다() = runTest(mainDispatcherRule.testDispatcher) {
        Achievement.entries.filter { it.isStreakBased }.forEach { achievement ->
            val viewModel = createViewModel()
            setPendingAchievement(viewModel, achievement)
            val activity = mockk<Activity>()
            coEvery { reviewService.requestReviewIfEligible(activity) } returns Unit

            viewModel.dismissAchievement(activity)

            assertNull(viewModel.pendingAchievement.value)
            coVerify { reviewService.requestReviewIfEligible(activity) }
        }
    }

    @Test
    fun dismissAchievement_연속기록업적이어도activity가없으면리뷰요청안한다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        setPendingAchievement(viewModel, Achievement.STREAK_7)

        viewModel.dismissAchievement(activity = null)

        assertNull(viewModel.pendingAchievement.value)
        coVerify(exactly = 0) { reviewService.requestReviewIfEligible(any()) }
    }

    @Test
    fun dismissAchievement_연속기록업적이아니면리뷰요청안한다() = runTest(mainDispatcherRule.testDispatcher) {
        Achievement.entries.filter { !it.isStreakBased }.forEach { achievement ->
            val viewModel = createViewModel()
            setPendingAchievement(viewModel, achievement)
            val activity = mockk<Activity>()

            viewModel.dismissAchievement(activity)

            coVerify(exactly = 0) { reviewService.requestReviewIfEligible(any()) }
        }
    }

    @Test
    fun onHomeScreenOpened_리뷰요청여부를reviewService에위임한다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        val activity = mockk<Activity>()
        coEvery { reviewService.requestReviewIfEligible(activity) } returns Unit

        viewModel.onHomeScreenOpened(activity)

        coVerify { reviewService.requestReviewIfEligible(activity) }
    }

    @Test
    fun undoLastEntry_repository에서제거하고스낵바를비운다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        coEvery { waterService.undoLastEntry(8, 200) } returns record

        viewModel.uiState.test {
            awaitItem()
            viewModel.undoLastEntry()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { waterService.undoLastEntry(8, 200) }
        assertNull(viewModel.snackbarMessage.value)
    }

    @Test
    fun undoLastEntry_취소된기록으로streak롤백을시도한다() = runTest(mainDispatcherRule.testDispatcher) {
        // 취소로 목표 미달성이 된 오늘 기록 — 이미 올라간 streak을 되돌려야 하는 상황
        val undoneRecord = DayRecord(dateKey = "2026-07-02", goal = 8, entries = emptyList())
        val viewModel = createViewModel()
        coEvery { waterService.undoLastEntry(8, 200) } returns undoneRecord

        viewModel.uiState.test {
            awaitItem()
            viewModel.undoLastEntry()
            cancelAndIgnoreRemainingEvents()
        }

        // uiState의 streak(취소 시점 기준 값)을 그대로 넘겨 롤백을 요청하는지 확인
        coVerify { waterService.rollbackStreakAfterUndo(undoneRecord, streak) }
    }

    @Test
    fun clearSnackbar_메시지를비운다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.clearSnackbar()

        assertNull(viewModel.snackbarMessage.value)
    }

    // pendingAchievement는 addWater 경로로만 채워지므로, dismiss 단독 테스트를 위해
    // achievementChecker가 해당 업적을 반환하도록 stub한 뒤 addWater를 한 번 거쳐 상태를 만든다.
    private suspend fun setPendingAchievement(viewModel: HomeViewModel, achievement: Achievement) {
        val updated = record.copy()
        coEvery { waterService.addWater(200, DrinkType.WATER, 8, 200) } returns WaterUpdateResult(record, updated)
        coEvery { waterService.updateStreak(updated, streak) } returns streak
        coEvery { achievementChecker.check(any(), updated, streak) } returns achievement

        viewModel.uiState.test {
            awaitItem()
            viewModel.addWater()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
