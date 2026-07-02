package com.watering.app.features.onboarding

import app.cash.turbine.test
import com.watering.app.core.data.SettingsRepository
import com.watering.app.core.model.UserSettings
import com.watering.app.core.service.NotificationService
import com.watering.app.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OnboardingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var notificationService: NotificationService
    private lateinit var settingsFlow: MutableStateFlow<UserSettings>

    private fun createViewModel(): OnboardingViewModel {
        settingsFlow = MutableStateFlow(UserSettings())
        settingsRepository = mockk {
            every { userSettings } returns settingsFlow
            coEvery { updateSettings(any()) } returns Unit
        }
        notificationService = mockk(relaxed = true)
        return OnboardingViewModel(settingsRepository, notificationService)
    }

    @Test
    fun isOnboardingDone_초기에는설정값을그대로반영한다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.isOnboardingDone.test {
            assertEquals(false, awaitItem())
        }
    }

    @Test
    fun isOnboardingDone_온보딩완료된설정이면true() = runTest(mainDispatcherRule.testDispatcher) {
        settingsFlow = MutableStateFlow(UserSettings(isOnboardingDone = true))
        settingsRepository = mockk {
            every { userSettings } returns settingsFlow
        }
        notificationService = mockk(relaxed = true)
        val viewModel = OnboardingViewModel(settingsRepository, notificationService)

        viewModel.isOnboardingDone.test {
            assertEquals(true, awaitItem())
        }
    }

    @Test
    fun completeOnboarding_설정을저장하고onboardingDone을true로만든다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        val slot = slot<UserSettings>()
        coEvery { settingsRepository.updateSettings(capture(slot)) } returns Unit

        viewModel.completeOnboarding(dailyGoal = 10, cupSize = 300, notificationEnabled = true)

        assertEquals(10, slot.captured.dailyGoal)
        assertEquals(300, slot.captured.cupSize)
        assertTrue(slot.captured.isOnboardingDone)
        assertTrue(slot.captured.notificationEnabled)
    }

    @Test
    fun completeOnboarding_알림활성화면알림을예약한다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.completeOnboarding(dailyGoal = 8, cupSize = 200, notificationEnabled = true)

        verify { notificationService.scheduleReminders(any()) }
    }

    @Test
    fun completeOnboarding_알림비활성화면알림을예약하지않는다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.completeOnboarding(dailyGoal = 8, cupSize = 200, notificationEnabled = false)

        verify(exactly = 0) { notificationService.scheduleReminders(any()) }
    }
}
