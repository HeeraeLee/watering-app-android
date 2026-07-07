package com.watering.app.features.stats

import app.cash.turbine.test
import com.watering.app.core.data.SettingsRepository
import com.watering.app.core.data.WaterRepository
import com.watering.app.core.model.DailyAchievement
import com.watering.app.core.model.DayRecord
import com.watering.app.core.model.DrinkType
import com.watering.app.core.model.StreakInfo
import com.watering.app.core.model.UserSettings
import com.watering.app.core.model.WaterEntry
import com.watering.app.testutil.MainDispatcherRule
import com.watering.app.core.service.TimeOfDayInsightResult
import io.mockk.every
import io.mockk.mockk
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SmartStatsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val todayDate = LocalDate.now()
    private val todayKey = todayDate.format(formatter)

    private fun entryAt(daysAgo: Long, hour: Int) = WaterEntry(
        timestampMillis = todayDate.minusDays(daysAgo).atTime(hour, 0)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        amount = 200,
        drinkType = DrinkType.WATER
    )

    private fun createViewModel(
        history: Map<String, DayRecord> = emptyMap(),
        annualHistory: Map<String, DailyAchievement> = emptyMap(),
        today: DayRecord = DayRecord(dateKey = todayKey, goal = 8),
        settings: UserSettings = UserSettings(dailyGoal = 8, notificationStart = 8, notificationEnd = 22)
    ): SmartStatsViewModel {
        val waterRepository = mockk<WaterRepository> {
            every { getHistory() } returns MutableStateFlow(history)
            every { getAnnualHistory() } returns MutableStateFlow(annualHistory)
            every { todayRecord } returns MutableStateFlow(today)
            every { streakInfo } returns MutableStateFlow(StreakInfo())
        }
        val settingsRepository = mockk<SettingsRepository> {
            every { userSettings } returns MutableStateFlow(settings)
        }
        return SmartStatsViewModel(waterRepository, settingsRepository, Clock.systemDefaultZone())
    }

    @Test
    fun uiState_최근30일통계를만들고오늘이마지막항목이다() = runTest(mainDispatcherRule.testDispatcher) {
        val today = DayRecord(dateKey = todayKey, entries = List(5) { entryAt(0, 9) }, goal = 8)
        val viewModel = createViewModel(today = today)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(30, state.monthStats.size)
            val lastStat = state.monthStats.last()
            assertTrue(lastStat.isToday)
            assertEquals(todayKey, lastStat.dateKey)
            assertEquals(5, lastStat.count)
        }
    }

    @Test
    fun uiState_데이터가부족하면인사이트는데이터부족상태다() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(TimeOfDayInsightResult.InsufficientData, awaitItem().insight)
        }
    }

    @Test
    fun uiState_뚜렷한시간대공백이있으면인사이트를반환한다() = runTest(mainDispatcherRule.testDispatcher) {
        val history = (1..10L).associate { daysAgo ->
            val key = todayDate.minusDays(daysAgo).format(formatter)
            key to DayRecord(
                dateKey = key,
                entries = listOf(8, 10, 12, 16, 18, 20).map { hour -> entryAt(daysAgo, hour) },
                goal = 8
            )
        }
        val viewModel = createViewModel(history = history)

        viewModel.uiState.test {
            val insight = awaitItem().insight
            assertEquals(TimeOfDayInsightResult.Found(startHour = 14, endHour = 16), insight)
        }
    }

    @Test
    fun uiState_연간슬롯은365개이며기록없는날은null이다() = runTest(mainDispatcherRule.testDispatcher) {
        val someDate = todayDate.minusDays(10).format(formatter)
        val annualHistory = mapOf(someDate to DailyAchievement(dateKey = someDate, totalCount = 8, goal = 8))
        val viewModel = createViewModel(annualHistory = annualHistory)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(365, state.annualDays.size)
            val recorded = state.annualDays.first { it?.dateKey == someDate }
            assertEquals(8, recorded?.totalCount)
            val missingDayIndex = state.annualDays.indexOfFirst {
                it == null
            }
            assertTrue("최소 하나 이상의 빈 날짜가 있어야 한다", missingDayIndex >= 0)
        }
    }

    @Test
    fun uiState_오늘의연간집계는실시간todayRecord를반영한다() = runTest(mainDispatcherRule.testDispatcher) {
        val today = DayRecord(dateKey = todayKey, entries = List(3) { entryAt(0, 9) }, goal = 8)
        val viewModel = createViewModel(today = today)

        viewModel.uiState.test {
            val state = awaitItem()
            val todayAchievement = state.annualDays.last()
            assertEquals(todayKey, todayAchievement?.dateKey)
            assertEquals(3, todayAchievement?.totalCount)
        }
    }

    @Test
    fun uiState_오늘의수분환산량을음료별환산율을적용해계산한다() = runTest(mainDispatcherRule.testDispatcher) {
        // 물 200ml(1.0) + 커피 200ml(0.7) = 200 + 140 = 340ml
        val today = DayRecord(
            dateKey = todayKey,
            entries = listOf(
                WaterEntry(timestampMillis = 0L, amount = 200, drinkType = DrinkType.WATER),
                WaterEntry(timestampMillis = 0L, amount = 200, drinkType = DrinkType.COFFEE)
            ),
            goal = 8
        )
        val viewModel = createViewModel(today = today)

        viewModel.uiState.test {
            assertEquals(340, awaitItem().todayHydrationVolumeMl)
        }
    }
}
