package com.watering.app.features.settings

import android.content.Context
import app.cash.turbine.test
import com.watering.app.R
import com.watering.app.core.data.SettingsRepository
import com.watering.app.core.model.DayRecord
import com.watering.app.core.model.DrinkType
import com.watering.app.core.model.UserSettings
import com.watering.app.core.model.WaterEntry
import com.watering.app.core.model.WidgetTheme
import com.watering.app.core.service.CsvExportService
import com.watering.app.core.service.HealthConnectAvailability
import com.watering.app.core.service.HealthConnectService
import com.watering.app.core.service.NotificationService
import com.watering.app.core.service.StatsInsightService
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
    fun updateDailyGoal_목표가바뀌면streak도동기화한다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel(UserSettings(dailyGoal = 8))

        viewModel.settings.test {
            awaitItem()
            viewModel.updateDailyGoal(6)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { waterService.syncStreakForGoalChange(6) }
    }

    @Test
    fun updateNotificationEnabled_목표가안바뀌면streak동기화를호출하지않는다() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel(UserSettings(dailyGoal = 8, notificationEnabled = true))

            viewModel.settings.test {
                awaitItem()
                viewModel.updateNotificationEnabled(false)
                cancelAndIgnoreRemainingEvents()
            }

            coVerify(exactly = 0) { waterService.syncStreakForGoalChange(any()) }
        }

    private fun todayRecordOf(entries: List<WaterEntry>) =
        DayRecord(dateKey = "2026-07-16", entries = entries)

    private fun entryOf(amount: Int) = WaterEntry(
        timestampMillis = 0L,
        amount = amount,
        drinkType = DrinkType.WATER
    )

    @Test
    fun requestCupSizeChange_오늘기록이없으면바로적용되고위젯은갱신하지않는다() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel(UserSettings(cupSize = 200))
            coEvery { waterService.currentTodayRecord() } returns todayRecordOf(emptyList())
            val slot = slot<UserSettings>()
            coEvery { settingsRepository.updateSettings(capture(slot)) } returns Unit

            viewModel.settings.test {
                awaitItem()
                viewModel.requestCupSizeChange(350)
                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(350, slot.captured.cupSize)
            assertEquals(CupSizeChangeUiState.Idle, viewModel.cupSizeChangeUiState.value)
            coVerify(exactly = 0) { widgetUpdater.updateAll() }
        }

    @Test
    fun requestCupSizeChange_오늘기록이있으면확인다이얼로그를띄우고설정은아직안바뀐다() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel(UserSettings(cupSize = 200))
            coEvery { waterService.currentTodayRecord() } returns
                todayRecordOf(listOf(entryOf(200), entryOf(200)))

            // settings는 WhileSubscribed(5_000)라 실제 구독자가 붙기 전까진 .value가 stateIn의
            // 시드 기본값(UserSettings())에 머물러 있다 — .test{}로 먼저 구독해 initialSettings가
            // 실제로 반영된 뒤에 requestCupSizeChange를 호출해야 settings.value.cupSize 비교가
            // 의도한 값(200)을 본다.
            viewModel.settings.test {
                awaitItem()
                viewModel.requestCupSizeChange(887)
                cancelAndIgnoreRemainingEvents()
            }

            val state = viewModel.cupSizeChangeUiState.value
            check(state is CupSizeChangeUiState.Confirming)
            assertEquals(887, state.newSize)
            assertEquals(400, state.todayTotalMl)
            assertEquals(2, state.todayCount)
            coVerify(exactly = 0) { settingsRepository.updateSettings(any()) }
        }

    @Test
    fun requestCupSizeChange_같은크기를선택하면아무일도안한다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel(UserSettings(cupSize = 200))

        viewModel.settings.test {
            awaitItem()
            viewModel.requestCupSizeChange(200)
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(CupSizeChangeUiState.Idle, viewModel.cupSizeChangeUiState.value)
        coVerify(exactly = 0) { waterService.currentTodayRecord() }
    }

    @Test
    fun confirmCupSizeChange_초기화선택시오늘기록을지우고체중목표라면초기화후기준으로목표를재계산한다() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel(UserSettings(cupSize = 355, dailyGoal = 6, weightKg = 60.0))
            // 초기화 전 조회(다이얼로그 표시용)엔 이미 마신 6잔이 잡히지만, 초기화 이후 재조회에서는
            // 빈 기록이어야 한다 — resetToday() 후 목표 재계산이 초기화 전 값을 잘못 재사용하지
            // 않는지(=recommendedGoalCups와 완전히 동일한 값이 나오는지) 검증
            coEvery { waterService.currentTodayRecord() } returns
                todayRecordOf(List(6) { entryOf(355) }) andThen todayRecordOf(emptyList())
            val slot = slot<UserSettings>()
            coEvery { settingsRepository.updateSettings(capture(slot)) } returns Unit

            viewModel.settings.test {
                awaitItem()
                viewModel.requestCupSizeChange(200)
                viewModel.confirmCupSizeChange(resetToday = true)
                cancelAndIgnoreRemainingEvents()
            }

            coVerify { waterService.resetToday() }
            assertEquals(200, slot.captured.cupSize)
            assertEquals(StatsInsightService.recommendedGoalCups(60.0, 200), slot.captured.dailyGoal)
            assertEquals(CupSizeChangeUiState.Idle, viewModel.cupSizeChangeUiState.value)
        }

    @Test
    fun confirmCupSizeChange_기록유지선택시체중목표라면이미마신양을반영해목표를재계산한다() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel(UserSettings(cupSize = 355, dailyGoal = 6, weightKg = 60.0))
            // 355ml x 6잔 = 2130ml로 이미 목표(1980ml)를 초과 달성한 상태에서 200ml로 줄여도
            // 목표가 6잔 아래로 내려가지 않아야 한다(달성 상태 역전 방지)
            val entries = List(6) { entryOf(355) }
            coEvery { waterService.currentTodayRecord() } returns todayRecordOf(entries)
            val slot = slot<UserSettings>()
            coEvery { settingsRepository.updateSettings(capture(slot)) } returns Unit

            viewModel.settings.test {
                awaitItem()
                viewModel.requestCupSizeChange(200)
                viewModel.confirmCupSizeChange(resetToday = false)
                cancelAndIgnoreRemainingEvents()
            }

            coVerify(exactly = 0) { waterService.resetToday() }
            assertEquals(200, slot.captured.cupSize)
            assertEquals(6, slot.captured.dailyGoal)
            coVerify { widgetUpdater.updateAll() }
        }

    @Test
    fun confirmCupSizeChange_체중목표가없으면목표잔수는안바뀐다() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel(UserSettings(cupSize = 200, dailyGoal = 8))
            coEvery { waterService.currentTodayRecord() } returns todayRecordOf(listOf(entryOf(200)))
            val slot = slot<UserSettings>()
            coEvery { settingsRepository.updateSettings(capture(slot)) } returns Unit

            viewModel.settings.test {
                awaitItem()
                viewModel.requestCupSizeChange(887)
                viewModel.confirmCupSizeChange(resetToday = false)
                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(887, slot.captured.cupSize)
            assertEquals(8, slot.captured.dailyGoal)
        }

    @Test
    fun dismissCupSizeChangeDialog_설정이안바뀐다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel(UserSettings(cupSize = 200))
        coEvery { waterService.currentTodayRecord() } returns todayRecordOf(listOf(entryOf(200)))

        viewModel.settings.test {
            awaitItem()
            viewModel.requestCupSizeChange(887)
            viewModel.dismissCupSizeChangeDialog()
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(CupSizeChangeUiState.Idle, viewModel.cupSizeChangeUiState.value)
        coVerify(exactly = 0) { settingsRepository.updateSettings(any()) }
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
    fun applyWeightGoal_적용하면스낵바메시지를설정한다() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel(UserSettings(dailyGoal = 8, cupSize = 200))
            every {
                context.getString(R.string.settings_weight_goal_applied_snackbar, "60", 10)
            } returns "60kg 기준 하루 목표를 10잔으로 설정했어요"

            viewModel.settings.test {
                awaitItem()
                viewModel.onWeightInputChange("60")
                viewModel.applyWeightGoal()
                cancelAndIgnoreRemainingEvents()
            }

            assertEquals("60kg 기준 하루 목표를 10잔으로 설정했어요", viewModel.snackbarMessage.value)
        }

    @Test
    fun weightGoalSubtitle_저장된몸무게있으면마지막입력을표시한다() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel(UserSettings(cupSize = 200, weightKg = 60.0))
            every {
                context.getString(R.string.settings_weight_goal_last_input, "60", 10)
            } returns "마지막 입력: 60kg → 하루 10잔"

            viewModel.weightGoalSubtitle.test {
                assertEquals("마지막 입력: 60kg → 하루 10잔", awaitItem())
            }
        }

    @Test
    fun weightGoalSubtitle_저장된몸무게없으면기본안내문을표시한다() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel(UserSettings(weightKg = null))
            every {
                context.getString(R.string.settings_weight_goal_subtitle)
            } returns "몸무게를 입력하면 하루 목표를 계산해드려요"

            viewModel.weightGoalSubtitle.test {
                assertEquals("몸무게를 입력하면 하루 목표를 계산해드려요", awaitItem())
            }
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
