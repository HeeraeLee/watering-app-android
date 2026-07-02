package com.watering.app.features.settings

import android.content.Context
import app.cash.turbine.test
import com.watering.app.core.data.SettingsRepository
import com.watering.app.core.model.UserSettings
import com.watering.app.core.service.NotificationService
import com.watering.app.core.service.WaterService
import com.watering.app.testutil.MainDispatcherRule
import com.watering.app.widget.WateringWidgetUpdater
import io.mockk.coEvery
import io.mockk.coVerify
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

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var context: Context
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var notificationService: NotificationService
    private lateinit var waterService: WaterService
    private lateinit var widgetUpdater: WateringWidgetUpdater

    private fun createViewModel(initialSettings: UserSettings = UserSettings()): SettingsViewModel {
        context = mockk(relaxed = true)
        settingsRepository = mockk {
            every { userSettings } returns MutableStateFlow(initialSettings)
            coEvery { updateSettings(any()) } returns Unit
        }
        notificationService = mockk(relaxed = true)
        waterService = mockk(relaxed = true)
        widgetUpdater = mockk(relaxed = true)
        return SettingsViewModel(context, settingsRepository, notificationService, waterService, widgetUpdater)
    }

    @Test
    fun settings_초기값은_repository값을반영한다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel(UserSettings(dailyGoal = 12))

        viewModel.settings.test {
            assertEquals(12, awaitItem().dailyGoal)
        }
    }

    // JVM 단위 테스트에서는 Build.VERSION.SDK_INT가 항상 0으로 취급되어
    // TIRAMISU(33) 미만 분기(무조건 허용)만 검증 가능하다. 실제 권한 거부 분기는
    // Robolectric/실기기 통합 테스트 영역.
    @Test
    fun notificationPermissionGranted_JVM테스트환경에서는SDK_INT가0이라항상true() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            assertTrue(viewModel.notificationPermissionGranted.value)

            viewModel.refreshNotificationPermission()
            assertTrue(viewModel.notificationPermissionGranted.value)
        }

    @Test
    fun updateDailyGoal_설정을저장하고위젯을갱신한다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel(UserSettings(dailyGoal = 8, notificationEnabled = true))
        val slot = slot<UserSettings>()
        coEvery { settingsRepository.updateSettings(capture(slot)) } returns Unit

        viewModel.settings.test {
            awaitItem()
            viewModel.updateDailyGoal(12)
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(12, slot.captured.dailyGoal)
        coVerify { widgetUpdater.updateAll() }
        verify { notificationService.scheduleReminders(slot.captured) }
    }

    @Test
    fun updateCupSize_위젯은갱신하지않는다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel(UserSettings(cupSize = 200))

        viewModel.settings.test {
            awaitItem()
            viewModel.updateCupSize(350)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 0) { widgetUpdater.updateAll() }
    }

    @Test
    fun updateNotificationEnabled_false로바꾸면알림을취소한다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel(UserSettings(notificationEnabled = true))

        viewModel.settings.test {
            awaitItem()
            viewModel.updateNotificationEnabled(false)
            cancelAndIgnoreRemainingEvents()
        }

        verify { notificationService.cancelReminders() }
        verify(exactly = 0) { notificationService.scheduleReminders(any()) }
    }

    @Test
    fun updateNotificationEnabled_true로바꾸면알림을예약한다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel(UserSettings(notificationEnabled = false))

        viewModel.settings.test {
            awaitItem()
            viewModel.updateNotificationEnabled(true)
            cancelAndIgnoreRemainingEvents()
        }

        verify { notificationService.scheduleReminders(any()) }
        verify(exactly = 0) { notificationService.cancelReminders() }
    }

    @Test
    fun resetAllData_waterService에전체삭제를위임한다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        coEvery { waterService.clearAllData() } returns Unit

        viewModel.resetAllData()

        coVerify { waterService.clearAllData() }
    }
}
