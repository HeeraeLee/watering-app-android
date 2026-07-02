package com.watering.app.features.stats

import app.cash.turbine.test
import com.watering.app.core.data.SettingsRepository
import com.watering.app.core.data.WaterRepository
import com.watering.app.core.model.DayRecord
import com.watering.app.core.model.DrinkType
import com.watering.app.core.model.StreakInfo
import com.watering.app.core.model.UserSettings
import com.watering.app.core.model.WaterEntry
import com.watering.app.testutil.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class StatsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val today = LocalDate.now()
    private val todayKey = today.format(formatter)
    private val yesterdayKey = today.minusDays(1).format(formatter)
    private val twoDaysAgoKey = today.minusDays(2).format(formatter)

    private fun entries(count: Int) = List(count) {
        WaterEntry(timestampMillis = 0L, amount = 200, drinkType = DrinkType.WATER)
    }

    private fun createViewModel(
        todayRecord: DayRecord,
        history: Map<String, DayRecord>,
        streak: StreakInfo = StreakInfo(currentStreak = 3, longestStreak = 5),
        settings: UserSettings = UserSettings(dailyGoal = 8)
    ): StatsViewModel {
        val waterRepository = mockk<WaterRepository> {
            every { getHistory() } returns MutableStateFlow(history)
            every { this@mockk.todayRecord } returns MutableStateFlow(todayRecord)
            every { streakInfo } returns MutableStateFlow(streak)
        }
        val settingsRepository = mockk<SettingsRepository> {
            every { userSettings } returns MutableStateFlow(settings)
        }
        return StatsViewModel(waterRepository, settingsRepository)
    }

    @Test
    fun uiState_최근7일통계를만들고오늘이마지막항목이다() = runTest(mainDispatcherRule.testDispatcher) {
        val todayRecord = DayRecord(dateKey = todayKey, entries = entries(5), goal = 8)
        val history = mapOf(
            yesterdayKey to DayRecord(dateKey = yesterdayKey, entries = entries(8), goal = 8),
            twoDaysAgoKey to DayRecord(dateKey = twoDaysAgoKey, entries = entries(3), goal = 8)
        )
        val viewModel = createViewModel(todayRecord, history)

        viewModel.uiState.test {
            val state = awaitItem()

            assertEquals(7, state.weekStats.size)
            val todayStat = state.weekStats.last()
            assertTrue(todayStat.isToday)
            assertEquals(todayKey, todayStat.dateKey)
            assertEquals(5, todayStat.count)
        }
    }

    @Test
    fun uiState_목표달성한날짜수를집계한다() = runTest(mainDispatcherRule.testDispatcher) {
        val todayRecord = DayRecord(dateKey = todayKey, entries = entries(5), goal = 8) // 미달성
        val history = mapOf(
            yesterdayKey to DayRecord(dateKey = yesterdayKey, entries = entries(8), goal = 8), // 달성
            twoDaysAgoKey to DayRecord(dateKey = twoDaysAgoKey, entries = entries(3), goal = 8) // 미달성
        )
        val viewModel = createViewModel(todayRecord, history)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.goalDays)
        }
    }

    @Test
    fun uiState_주간총합과평균을계산한다() = runTest(mainDispatcherRule.testDispatcher) {
        val todayRecord = DayRecord(dateKey = todayKey, entries = entries(5), goal = 8)
        val history = mapOf(
            yesterdayKey to DayRecord(dateKey = yesterdayKey, entries = entries(8), goal = 8),
            twoDaysAgoKey to DayRecord(dateKey = twoDaysAgoKey, entries = entries(3), goal = 8)
        )
        val viewModel = createViewModel(todayRecord, history)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(16, state.weeklyTotal) // 5 + 8 + 3 + (나머지 4일 0)
            assertEquals(16.0 / 7.0, state.weeklyAvg, 0.0001)
        }
    }

    @Test
    fun uiState_기록없는날은0으로집계된다() = runTest(mainDispatcherRule.testDispatcher) {
        val todayRecord = DayRecord(dateKey = todayKey, entries = emptyList(), goal = 8)
        val viewModel = createViewModel(todayRecord, history = emptyMap())

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(0, state.weeklyTotal)
            assertEquals(0, state.goalDays)
            assertTrue(state.weekStats.all { it.count == 0 })
        }
    }

    @Test
    fun uiState_streak정보를그대로전달한다() = runTest(mainDispatcherRule.testDispatcher) {
        val todayRecord = DayRecord(dateKey = todayKey, entries = emptyList(), goal = 8)
        val viewModel = createViewModel(
            todayRecord,
            history = emptyMap(),
            streak = StreakInfo(currentStreak = 7, longestStreak = 20)
        )

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(7, state.currentStreak)
            assertEquals(20, state.longestStreak)
        }
    }
}
