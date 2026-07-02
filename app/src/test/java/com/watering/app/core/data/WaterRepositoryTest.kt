package com.watering.app.core.data

import com.watering.app.core.datastore.WaterDataStore
import com.watering.app.core.model.DayRecord
import com.watering.app.core.model.StreakInfo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

        val result = repository.updateStreak(notAchieved, current, isPremium = false)

        assertSame(current, result)
        coVerify(exactly = 0) { dataStore.saveStreakInfo(any()) }
    }

    @Test
    fun updateStreak_기록날짜가오늘이아니면_기존streak그대로반환() = runTest {
        val pastRecord = achievedRecord(dateKey = yesterdayKey)
        val current = StreakInfo(currentStreak = 5)

        val result = repository.updateStreak(pastRecord, current, isPremium = false)

        assertSame(current, result)
        coVerify(exactly = 0) { dataStore.saveStreakInfo(any()) }
    }

    @Test
    fun updateStreak_연속기록없다가오늘첫달성_streak1로시작() = runTest {
        val current = StreakInfo(currentStreak = 0, longestStreak = 0)

        val result = repository.updateStreak(achievedRecord(), current, isPremium = false)

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

        val result = repository.updateStreak(achievedRecord(), current, isPremium = false)

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

        val result = repository.updateStreak(achievedRecord(), current, isPremium = false)

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

        val result = repository.updateStreak(achievedRecord(), current, isPremium = false)

        assertEquals(4, result.currentStreak)
    }

    @Test
    fun updateStreak_이틀전달성_비프리미엄이면_streak가1로리셋() = runTest {
        val current = StreakInfo(
            currentStreak = 10,
            longestStreak = 10,
            lastAchievedDateKey = twoDaysAgoKey
        )

        val result = repository.updateStreak(achievedRecord(), current, isPremium = false)

        assertEquals(1, result.currentStreak)
        assertFalse(result.protectionUsedThisMonth)
    }

    @Test
    fun updateStreak_이틀전달성_프리미엄이고보호미사용이면_streak가보호되어1증가() = runTest {
        val current = StreakInfo(
            currentStreak = 10,
            longestStreak = 10,
            lastAchievedDateKey = twoDaysAgoKey,
            protectionUsedThisMonth = false
        )

        val result = repository.updateStreak(achievedRecord(), current, isPremium = true)

        assertEquals(11, result.currentStreak)
        assertTrue(result.protectionUsedThisMonth)
        assertEquals(currentMonthKey, result.protectionUsedMonthKey)
    }

    @Test
    fun updateStreak_이틀전달성_프리미엄이지만이번달보호이미사용했으면_streak가1로리셋() = runTest {
        val current = StreakInfo(
            currentStreak = 10,
            longestStreak = 10,
            lastAchievedDateKey = twoDaysAgoKey,
            protectionUsedThisMonth = true,
            protectionUsedMonthKey = currentMonthKey
        )

        val result = repository.updateStreak(achievedRecord(), current, isPremium = true)

        assertEquals(1, result.currentStreak)
    }

    @Test
    fun updateStreak_삼일이상공백은_프리미엄이어도보호대상아니라streak가1로리셋() = runTest {
        val threeDaysAgoKey = today.minusDays(3).format(formatter)
        val current = StreakInfo(
            currentStreak = 10,
            longestStreak = 10,
            lastAchievedDateKey = threeDaysAgoKey
        )

        val result = repository.updateStreak(achievedRecord(), current, isPremium = true)

        assertEquals(1, result.currentStreak)
    }

    @Test
    fun resetToday_dataStore위임하고완료된다() = runTest {
        coEvery { dataStore.resetTodayRecord() } returns Unit

        repository.resetToday()

        coVerify { dataStore.resetTodayRecord() }
    }

    @Test
    fun clearAllData_dataStore위임하고완료된다() = runTest {
        coEvery { dataStore.clearAllData() } returns Unit

        repository.clearAllData()

        coVerify { dataStore.clearAllData() }
    }
}
