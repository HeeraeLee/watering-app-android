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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
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
    }

    // 로직 헛점 전수 분석 ⑧ 재현(2026-07-16): 알림과 무관한 설정(목표 등)을 바꿀 때마다
    // scheduleReminders가 매번 호출돼 WorkManager의 REPLACE 정책으로 알림 flex 타이머가 그때마다
    // 리셋되던 문제 — 알림 관련 필드(활성 여부/주기/시작·종료 시각)가 실제로 바뀌지 않았다면
    // 재스케줄을 호출하지 않아야 한다.
    @Test
    fun updateDailyGoal_알림설정이안바뀌면알림을재스케줄하지않는다() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel(UserSettings(dailyGoal = 8, notificationEnabled = true))

            viewModel.settings.test {
                awaitItem()
                viewModel.updateDailyGoal(12)
                cancelAndIgnoreRemainingEvents()
            }

            verify(exactly = 0) { notificationService.scheduleReminders(any()) }
            verify(exactly = 0) { notificationService.cancelReminders() }
        }

    // 로직 헛점 전수 분석 ⑧ 재현(재확인 필요로 남아있던 항목): update()가 매번 settings.value(캐시된
    // 스냅샷)를 읽어 그중 한 필드만 바꾼 UserSettings 전체를 덮어쓰는 구조라, 서로 다른 설정을
    // 거의 동시에 바꾸면 두 번째 호출이 첫 번째 호출의 DataStore 쓰기가 아직 settings.value에
    // 반영되기 전의 stale한 previous를 읽어 그 필드 변경을 덮어써버릴 수 있었다(lost update).
    // settingsRepository.updateSettings가 실제 DataStore처럼 지연 후 다시 userSettings에 반영되도록
    // 흉내낸 뒤, 두 update()를 겹치는 순서로 호출해 재현 — mutex로 직렬화한 뒤에는 두 필드 변경이
    // 모두 살아남아야 한다.
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun update_서로다른설정을거의동시에바꿔도서로의변경을덮어쓰지않는다() =
        runTest(mainDispatcherRule.testDispatcher) {
            val backing = MutableStateFlow(
                UserSettings(dailyGoal = 8, cupSize = 200, widgetTheme = WidgetTheme.DEFAULT)
            )
            context = mockk(relaxed = true)
            val writtenSettings = slot<UserSettings>()
            settingsRepository = mockk {
                every { userSettings } returns backing
                coEvery { updateSettings(capture(writtenSettings)) } coAnswers {
                    // DataStore 쓰기 → Flow 재방출까지의 실제 왕복 지연을 흉내내, 두 update() 호출이
                    // 겹치는 창을 인위적으로 만든다
                    delay(10)
                    backing.value = writtenSettings.captured
                }
            }
            notificationService = mockk(relaxed = true)
            waterService = mockk(relaxed = true)
            widgetUpdater = mockk(relaxed = true)
            healthConnectService = mockk { every { availability } returns HealthConnectAvailability.AVAILABLE }
            csvExportService = mockk(relaxed = true)
            val viewModel = SettingsViewModel(
                context, settingsRepository, notificationService, waterService,
                widgetUpdater, healthConnectService, csvExportService
            )

            viewModel.settings.test {
                awaitItem()
                viewModel.updateDailyGoal(12)
                viewModel.updateWidgetTheme(WidgetTheme.MINT)
                cancelAndIgnoreRemainingEvents()
            }
            advanceUntilIdle()

            assertEquals(12, backing.value.dailyGoal)
            assertEquals(WidgetTheme.MINT, backing.value.widgetTheme)
        }

    @Test
    fun updateDailyGoal_목표가바뀌면streak도동기화한다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel(UserSettings(dailyGoal = 8))

        viewModel.settings.test {
            awaitItem()
            viewModel.updateDailyGoal(6)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { waterService.syncStreakForGoalChange(6, 200) }
    }

    @Test
    fun updateNotificationEnabled_목표와컵크기가안바뀌면streak동기화를호출하지않는다() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel(UserSettings(dailyGoal = 8, notificationEnabled = true))

            viewModel.settings.test {
                awaitItem()
                viewModel.updateNotificationEnabled(false)
                cancelAndIgnoreRemainingEvents()
            }

            coVerify(exactly = 0) { waterService.syncStreakForGoalChange(any(), any()) }
        }

    // 오너 제보 재현(조사 결과 ③): 체중 미설정 상태에서 컵 크기만 줄이면 목표 잔수는 그대로라
    // 예전 코드는 streak 동기화 자체를 호출하지 않았다 — 그 사이 오늘 기록이 새 컵 크기 기준으로
    // 재분모돼 실제로는 부족한데도 화면상 달성으로 보일 수 있었다. 이제는 컵 크기만 바뀌어도
    // 동기화를 호출해야 한다.
    @Test
    fun applyCupSizeChange_목표잔수는그대로여도컵크기가바뀌면streak을동기화한다() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel(UserSettings(cupSize = 200, dailyGoal = 8))
            coEvery { waterService.currentTodayRecord() } returns todayRecordOf(listOf(entryOf(200)))

            viewModel.settings.test {
                awaitItem()
                viewModel.requestCupSizeChange(100)
                viewModel.confirmCupSizeChange(resetToday = false)
                cancelAndIgnoreRemainingEvents()
            }

            coVerify { waterService.syncStreakForGoalChange(8, 100) }
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

    // 오너 제보 재현(2026-07-16 발견): 체중을 처음 입력하면 오늘 이미 마신 기록을 전혀 고려하지
    // 않고 recommendedGoalCups를 그대로 목표로 대입해, 이미 달성한 상태가 미달성으로 역전될 수
    // 있었다. "다른 음료 선택"으로 컵 크기(200ml)와 무관하게 1200ml를 한 번에 기록해 이미
    // 6/6(totalCount = 1200/200 = 6.0) 달성한 상황 — 컵 크기 변경 경로와 동일하게
    // recommendedGoalCupsPreservingProgress를 재사용해야 새 목표(5)가 totalCount(6.0)를
    // 넘지 않아 달성 상태가 유지된다. 고치기 전에는 recommendedGoalCups(60.0, 200)인 10이
    // 그대로 쓰여 6/10(미달성)으로 역전됐다.
    @Test
    fun applyWeightGoal_오늘이미마신기록을반영해목표잔수를역전없이재계산한다() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel(UserSettings(dailyGoal = 6, cupSize = 200))
            val todayEntries = listOf(entryOf(1200))
            coEvery { waterService.currentTodayRecord() } returns todayRecordOf(todayEntries)
            val slot = slot<UserSettings>()
            coEvery { settingsRepository.updateSettings(capture(slot)) } returns Unit

            viewModel.settings.test {
                awaitItem()
                viewModel.onWeightInputChange("60")
                viewModel.applyWeightGoal()
                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(60.0, slot.captured.weightKg)
            assertEquals(
                StatsInsightService.recommendedGoalCupsPreservingProgress(60.0, 200, todayEntries),
                slot.captured.dailyGoal
            )
            assertTrue(slot.captured.dailyGoal < StatsInsightService.recommendedGoalCups(60.0, 200))
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
