package com.watering.app.core.data

import app.cash.turbine.test
import com.watering.app.core.datastore.WaterDataStore
import com.watering.app.core.model.DailyAchievement
import com.watering.app.core.model.DayRecord
import com.watering.app.core.model.StreakInfo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WaterRepositoryTest {

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
    private val today = LocalDate.now()
    private val todayKey = today.format(formatter)
    private val yesterdayKey = today.minusDays(1).format(formatter)
    private val twoDaysAgoKey = today.minusDays(2).format(formatter)
    private val currentMonthKey = today.format(monthFormatter)

    private lateinit var dataStore: WaterDataStore
    private lateinit var repository: WaterRepository

    private fun achievedRecord(dateKey: String = todayKey) = DayRecord(
        dateKey = dateKey,
        entries = emptyList(),
        goal = 0 // goal=0 이면 isAchieved는 totalCount(0) >= goal(0) 이므로 true
    )

    @Before
    fun setUp() {
        dataStore = mockk(relaxed = true)
        repository = WaterRepository(dataStore)
    }

    @Test
    fun updateStreak_기록미달성이면_기존streak그대로반환() = runTest {
        val notAchieved = DayRecord(dateKey = todayKey, entries = emptyList(), goal = 1)
        val current = StreakInfo(currentStreak = 5)

        val result = repository.updateStreak(notAchieved, current)

        assertSame(current, result)
        coVerify(exactly = 0) { dataStore.saveStreakInfo(any()) }
    }

    @Test
    fun updateStreak_시스템날짜가아닌record자체의dateKey를기준으로계산한다() = runTest {
        // record.dateKey가 어제(yesterdayKey)여도, 그 날짜를 기준으로 "어제"에 해당하는
        // twoDaysAgoKey에 달성했으면 streak가 이어져야 함 — LocalDate.now()(시스템 시각)를 쓰면
        // 자정 경계에서 이 레코드를 거부하거나 잘못된 날짜로 비교하는 버그가 재발함(v0.24.1과
        // 동일한 버그 패턴, WaterRepository에도 남아있던 것을 발견해 수정)
        val recordFromYesterday = achievedRecord(dateKey = yesterdayKey)
        val current = StreakInfo(currentStreak = 5, longestStreak = 5, lastAchievedDateKey = twoDaysAgoKey)

        val result = repository.updateStreak(recordFromYesterday, current)

        assertEquals(6, result.currentStreak)
        assertEquals(yesterdayKey, result.lastAchievedDateKey)
        coVerify { dataStore.saveStreakInfo(result) }
    }

    @Test
    fun updateStreak_연속기록없다가오늘첫달성_streak1로시작() = runTest {
        val current = StreakInfo(currentStreak = 0, longestStreak = 0)

        val result = repository.updateStreak(achievedRecord(), current)

        assertEquals(1, result.currentStreak)
        assertEquals(1, result.longestStreak)
        assertEquals(todayKey, result.lastAchievedDateKey)
        coVerify { dataStore.saveStreakInfo(result) }
    }

    @Test
    fun updateStreak_어제달성했으면_streak1증가() = runTest {
        val current = StreakInfo(
            currentStreak = 3,
            longestStreak = 5,
            lastAchievedDateKey = yesterdayKey
        )

        val result = repository.updateStreak(achievedRecord(), current)

        assertEquals(4, result.currentStreak)
        assertEquals(5, result.longestStreak) // 기존 최장기록 유지
    }

    @Test
    fun updateStreak_최장기록보다streak가높아지면_longestStreak도갱신() = runTest {
        val current = StreakInfo(
            currentStreak = 5,
            longestStreak = 5,
            lastAchievedDateKey = yesterdayKey
        )

        val result = repository.updateStreak(achievedRecord(), current)

        assertEquals(6, result.currentStreak)
        assertEquals(6, result.longestStreak)
    }

    @Test
    fun updateStreak_오늘이미달성처리된상태에서다시호출해도_streak변화없음() = runTest {
        val current = StreakInfo(
            currentStreak = 4,
            longestStreak = 4,
            lastAchievedDateKey = todayKey
        )

        val result = repository.updateStreak(achievedRecord(), current)

        assertEquals(4, result.currentStreak)
    }

    @Test
    fun updateStreak_이틀전달성_보호미사용이면_streak가보호되어1증가() = runTest {
        val current = StreakInfo(
            currentStreak = 10,
            longestStreak = 10,
            lastAchievedDateKey = twoDaysAgoKey,
            protectionUsedThisMonth = false
        )

        val result = repository.updateStreak(achievedRecord(), current)

        assertEquals(11, result.currentStreak)
        assertTrue(result.protectionUsedThisMonth)
        assertEquals(currentMonthKey, result.protectionUsedMonthKey)
    }

    @Test
    fun updateStreak_이틀전달성_이번달보호이미사용했으면_streak가1로리셋() = runTest {
        val current = StreakInfo(
            currentStreak = 10,
            longestStreak = 10,
            lastAchievedDateKey = twoDaysAgoKey,
            protectionUsedThisMonth = true,
            protectionUsedMonthKey = currentMonthKey
        )

        val result = repository.updateStreak(achievedRecord(), current)

        assertEquals(1, result.currentStreak)
    }

    @Test
    fun updateStreak_삼일이상공백은_보호대상아니라streak가1로리셋() = runTest {
        val threeDaysAgoKey = today.minusDays(3).format(formatter)
        val current = StreakInfo(
            currentStreak = 10,
            longestStreak = 10,
            lastAchievedDateKey = threeDaysAgoKey
        )

        val result = repository.updateStreak(achievedRecord(), current)

        assertEquals(1, result.currentStreak)
    }

    @Test
    fun rollbackStreakAfterUndo_취소후에도여전히달성상태면_아무것도안한다() = runTest {
        val stillAchieved = achievedRecord() // goal=0, entries=0개 → 항상 달성
        val current = StreakInfo(currentStreak = 5, lastAchievedDateKey = todayKey)

        val result = repository.rollbackStreakAfterUndo(stillAchieved, current)

        assertSame(current, result)
        coVerify(exactly = 0) { dataStore.saveStreakInfo(any()) }
    }

    @Test
    fun rollbackStreakAfterUndo_streak이오늘달성으로갱신된게아니면_아무것도안한다() = runTest {
        // lastAchievedDateKey가 오늘이 아니므로, 이 취소는 애초에 streak을 올린 원인이 아니었음
        val notAchieved = DayRecord(dateKey = todayKey, entries = emptyList(), goal = 1)
        val current = StreakInfo(currentStreak = 5, lastAchievedDateKey = yesterdayKey)

        val result = repository.rollbackStreakAfterUndo(notAchieved, current)

        assertSame(current, result)
        coVerify(exactly = 0) { dataStore.saveStreakInfo(any()) }
    }

    @Test
    fun rollbackStreakAfterUndo_어제도달성상태였으면_streak를1줄이고어제날짜로되돌린다() = runTest {
        val notAchieved = DayRecord(dateKey = todayKey, entries = emptyList(), goal = 1)
        val current = StreakInfo(currentStreak = 4, longestStreak = 10, lastAchievedDateKey = todayKey)
        every { dataStore.getAnnualHistory() } returns MutableStateFlow(
            mapOf(yesterdayKey to DailyAchievement(dateKey = yesterdayKey, totalCount = 8, goal = 8))
        )

        val result = repository.rollbackStreakAfterUndo(notAchieved, current)

        assertEquals(3, result.currentStreak)
        assertEquals(10, result.longestStreak) // 최장 기록과 값이 달랐으니 그대로 유지
        assertEquals(yesterdayKey, result.lastAchievedDateKey)
        coVerify { dataStore.saveStreakInfo(result) }
    }

    @Test
    fun rollbackStreakAfterUndo_이번갱신이막최장기록을세운것이었으면_최장기록도함께되돌린다() = runTest {
        val notAchieved = DayRecord(dateKey = todayKey, entries = emptyList(), goal = 1)
        val current = StreakInfo(currentStreak = 6, longestStreak = 6, lastAchievedDateKey = todayKey)
        every { dataStore.getAnnualHistory() } returns MutableStateFlow(
            mapOf(yesterdayKey to DailyAchievement(dateKey = yesterdayKey, totalCount = 8, goal = 8))
        )

        val result = repository.rollbackStreakAfterUndo(notAchieved, current)

        assertEquals(5, result.currentStreak)
        assertEquals(5, result.longestStreak)
    }

    @Test
    fun rollbackStreakAfterUndo_이틀전달성으로보호가쓰였던갱신이면_보호플래그도되돌린다() = runTest {
        val notAchieved = DayRecord(dateKey = todayKey, entries = emptyList(), goal = 1)
        val current = StreakInfo(
            currentStreak = 11,
            longestStreak = 11,
            lastAchievedDateKey = todayKey,
            protectionUsedThisMonth = true,
            protectionUsedMonthKey = currentMonthKey
        )
        every { dataStore.getAnnualHistory() } returns MutableStateFlow(
            mapOf(twoDaysAgoKey to DailyAchievement(dateKey = twoDaysAgoKey, totalCount = 8, goal = 8))
        )

        val result = repository.rollbackStreakAfterUndo(notAchieved, current)

        assertEquals(10, result.currentStreak)
        assertEquals(twoDaysAgoKey, result.lastAchievedDateKey)
        assertEquals(false, result.protectionUsedThisMonth)
    }

    @Test
    fun rollbackStreakAfterUndo_streak이1이었으면_0으로되돌아간다() = runTest {
        val notAchieved = DayRecord(dateKey = todayKey, entries = emptyList(), goal = 1)
        val current = StreakInfo(currentStreak = 1, longestStreak = 1, lastAchievedDateKey = todayKey)
        every { dataStore.getAnnualHistory() } returns MutableStateFlow(emptyMap())

        val result = repository.rollbackStreakAfterUndo(notAchieved, current)

        assertEquals(0, result.currentStreak)
    }

    @Test
    fun resetToday_dataStore위임하고완료된다() = runTest {
        val blank = DayRecord(dateKey = todayKey, goal = 8)
        coEvery { dataStore.resetTodayRecord(8) } returns blank

        val result = repository.resetToday(8)

        assertEquals(blank, result)
        coVerify { dataStore.resetTodayRecord(8) }
    }

    @Test
    fun resetTodayIfStale_dataStore위임하고결과를그대로반환한다() = runTest {
        val blank = DayRecord(dateKey = todayKey, goal = 8)
        coEvery { dataStore.resetTodayRecordIfStale(8) } returns blank

        val result = repository.resetTodayIfStale(8)

        assertEquals(blank, result)
        coVerify { dataStore.resetTodayRecordIfStale(8) }
    }

    @Test
    fun resetTodayIfStale_dataStore가null을반환하면그대로null을반환한다() = runTest {
        coEvery { dataStore.resetTodayRecordIfStale(8) } returns null

        val result = repository.resetTodayIfStale(8)

        assertEquals(null, result)
    }

    @Test
    fun clearAllData_dataStore위임하고완료된다() = runTest {
        coEvery { dataStore.clearAllData() } returns Unit

        repository.clearAllData()

        coVerify { dataStore.clearAllData() }
    }

    @Test
    fun restoreAll_annualHistory까지포함해dataStore위임하고완료된다() = runTest {
        val today = DayRecord(dateKey = todayKey, goal = 8)
        val streak = StreakInfo(currentStreak = 3)
        val history = mapOf(yesterdayKey to DayRecord(dateKey = yesterdayKey, goal = 8))
        val annualHistory = mapOf(
            yesterdayKey to DailyAchievement(dateKey = yesterdayKey, totalCount = 8, goal = 8)
        )
        coEvery { dataStore.restoreAll(today, streak, history, annualHistory) } returns Unit

        repository.restoreAll(today, streak, history, annualHistory)

        coVerify { dataStore.restoreAll(today, streak, history, annualHistory) }
    }

    @Test
    fun getAnnualHistory_dataStore의연간이력을그대로전달한다() = runTest {
        val annualHistory = mapOf(
            todayKey to DailyAchievement(dateKey = todayKey, totalCount = 8, goal = 8)
        )
        every { dataStore.getAnnualHistory() } returns MutableStateFlow(annualHistory)

        repository.getAnnualHistory().test {
            assertEquals(annualHistory, awaitItem())
        }
    }
}
