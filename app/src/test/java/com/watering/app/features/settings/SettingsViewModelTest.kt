package com.watering.app.features.settings

import android.content.Context
import app.cash.turbine.test
import com.watering.app.core.data.SettingsRepository
import com.watering.app.core.model.UserSettings
import com.watering.app.core.model.WidgetTheme
import com.watering.app.core.service.CsvExportService
import com.watering.app.core.service.HealthConnectAvailability
import com.watering.app.core.service.HealthConnectService
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
    private lateinit var healthConnectService: HealthConnectService
    private lateinit var csvExportService: CsvExportService

    private fun createViewModel(initialSettings: UserSettings = UserSettings()): SettingsViewModel {
        context = mockk(relaxed = true)
        settingsRepository = mockk {
            every { userSettings } returns MutableStateFlow(initialSettings)
            coEvery { updateSettings(any()) } returns Unit
        }
        notificationService = mockk(relaxed = true)
        waterService = mockk(relaxed = true)
        widgetUpdater = mockk(relaxed = true)
        healthConnectService = mockk {
            every { availability } returns HealthConnectAvailability.AVAILABLE
        }
        csvExportService = mockk(relaxed = true)
        return SettingsViewModel(
            context,
            settingsRepository,
            notificationService,
            waterService,
            widgetUpdater,
            healthConnectService,
            csvExportService
        )
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
    fun updateWidgetTheme_설정을저장하고위젯을갱신한다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel(UserSettings(widgetTheme = WidgetTheme.DEFAULT))
        val slot = slot<UserSettings>()
        coEvery { settingsRepository.updateSettings(capture(slot)) } returns Unit

        viewModel.settings.test {
            awaitItem()
            viewModel.updateWidgetTheme(WidgetTheme.MINT)
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(WidgetTheme.MINT, slot.captured.widgetTheme)
        coVerify { widgetUpdater.updateAll() }
    }

    @Test
    fun resetAllData_waterService에전체삭제를위임한다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        coEvery { waterService.clearAllData() } returns Unit

        viewModel.resetAllData()

        coVerify { waterService.clearAllData() }
    }

    @Test
    fun openWeightGoalDialog_저장된몸무게있으면입력값과추천잔수를채운다() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel(UserSettings(cupSize = 200, weightKg = 60.0))

            viewModel.settings.test {
                awaitItem()
                viewModel.openWeightGoalDialog()
                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(
                WeightGoalUiState.Editing(weightInput = "60", recommendedCups = 10),
                viewModel.weightGoalUiState.value
            )
        }

    @Test
    fun openWeightGoalDialog_저장된몸무게없으면빈입력으로연다() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel(UserSettings(weightKg = null))

            viewModel.openWeightGoalDialog()

            assertEquals(
                WeightGoalUiState.Editing(weightInput = "", recommendedCups = null),
                viewModel.weightGoalUiState.value
            )
        }

    @Test
    fun onWeightInputChange_유효한몸무게면추천잔수를계산한다() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel(UserSettings(cupSize = 200))

            viewModel.onWeightInputChange("60")

            assertEquals(
                WeightGoalUiState.Editing(weightInput = "60", recommendedCups = 10),
                viewModel.weightGoalUiState.value
            )
        }

    @Test
    fun onWeightInputChange_유효하지않으면추천잔수는null이다() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            viewModel.onWeightInputChange("abc")

            assertEquals(
                WeightGoalUiState.Editing(weightInput = "abc", recommendedCups = null),
                viewModel.weightGoalUiState.value
            )
        }

    @Test
    fun applyWeightGoal_몸무게와목표를저장하고Idle로되돌린다() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel(UserSettings(dailyGoal = 8, cupSize = 200))
            val slot = slot<UserSettings>()
            coEvery { settingsRepository.updateSettings(capture(slot)) } returns Unit

            viewModel.settings.test {
                awaitItem()
                viewModel.onWeightInputChange("60")
                viewModel.applyWeightGoal()
                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(60.0, slot.captured.weightKg)
            assertEquals(10, slot.captured.dailyGoal)
            assertEquals(WeightGoalUiState.Idle, viewModel.weightGoalUiState.value)
        }

    @Test
    fun dismissWeightGoalState_Idle로되돌린다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onWeightInputChange("60")

        viewModel.dismissWeightGoalState()

        assertEquals(WeightGoalUiState.Idle, viewModel.weightGoalUiState.value)
    }

    @Test
    fun onHydrationSyncToggle_false면바로저장하고끈다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel(UserSettings(healthConnectEnabled = true))
        val slot = slot<UserSettings>()
        coEvery { settingsRepository.updateSettings(capture(slot)) } returns Unit

        viewModel.settings.test {
            awaitItem()
            viewModel.onHydrationSyncToggle(false)
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(false, slot.captured.healthConnectEnabled)
    }

    @Test
    fun onHydrationSyncToggle_true인데SDK사용불가면NotAvailable상태가된다() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            every { healthConnectService.availability } returns HealthConnectAvailability.NOT_INSTALLED

            viewModel.onHydrationSyncToggle(true)

            assertEquals(
                HydrationSyncUiState.NotAvailable(HealthConnectAvailability.NOT_INSTALLED),
                viewModel.hydrationSyncUiState.value
            )
        }

    @Test
    fun onHydrationSyncToggle_true이고권한이미있으면바로저장한다() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            coEvery { healthConnectService.hasPermissions(HealthConnectService.HYDRATION_PERMISSIONS) } returns true
            val slot = slot<UserSettings>()
            coEvery { settingsRepository.updateSettings(capture(slot)) } returns Unit

            viewModel.settings.test {
                awaitItem()
                viewModel.onHydrationSyncToggle(true)
                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(true, slot.captured.healthConnectEnabled)
        }

    @Test
    fun onHydrationSyncToggle_true인데권한없으면NeedsPermission상태가된다() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            coEvery { healthConnectService.hasPermissions(HealthConnectService.HYDRATION_PERMISSIONS) } returns false

            viewModel.onHydrationSyncToggle(true)

            assertEquals(HydrationSyncUiState.NeedsPermission, viewModel.hydrationSyncUiState.value)
        }

    @Test
    fun onHydrationPermissionResult_승인되면저장한다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        val slot = slot<UserSettings>()
        coEvery { settingsRepository.updateSettings(capture(slot)) } returns Unit

        viewModel.settings.test {
            awaitItem()
            viewModel.onHydrationPermissionResult(HealthConnectService.HYDRATION_PERMISSIONS)
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(true, slot.captured.healthConnectEnabled)
    }

    @Test
    fun onHydrationPermissionResult_거부되면PermissionDenied상태가된다() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            viewModel.onHydrationPermissionResult(emptySet())

            assertEquals(HydrationSyncUiState.PermissionDenied, viewModel.hydrationSyncUiState.value)
        }
}
