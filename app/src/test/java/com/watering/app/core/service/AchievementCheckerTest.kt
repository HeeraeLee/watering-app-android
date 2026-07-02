package com.watering.app.core.service

import com.watering.app.core.datastore.AchievementDataStore
import com.watering.app.core.model.Achievement
import com.watering.app.core.model.DayRecord
import com.watering.app.core.model.StreakInfo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AchievementCheckerTest {

    private lateinit var dataStore: AchievementDataStore
    private lateinit var checker: AchievementChecker

    @Before
    fun setUp() {
        dataStore = mockk()
        checker = AchievementChecker(dataStore)
        coEvery { dataStore.isAlreadyEarned(any(), any()) } returns false
        coEvery { dataStore.markEarned(any(), any()) } returns Unit
    }

    private fun record(count: Int, goal: Int = 8) =
        DayRecord(dateKey = "2026-07-02", entries = List(count) { fakeEntry() }, goal = goal)

    private fun fakeEntry() = com.watering.app.core.model.WaterEntry(
        timestampMillis = 0L,
        amount = 200,
        drinkType = com.watering.app.core.model.DrinkType.WATER
    )

    @Test
    fun check_처음물마신경우_FIRST_SIP반환() = runTest {
        val result = checker.check(record(0), record(1), StreakInfo())

        assertEquals(Achievement.FIRST_SIP, result)
        coVerify { dataStore.markEarned(any(), Achievement.FIRST_SIP) }
    }

    @Test
    fun check_목표달성했지만streak가3미만이면_GOAL_ACHIEVED반환() = runTest {
        val prev = record(7, goal = 8)
        val next = record(8, goal = 8)

        val result = checker.check(prev, next, StreakInfo(currentStreak = 1))

        assertEquals(Achievement.GOAL_ACHIEVED, result)
    }

    @Test
    fun check_목표달성하고streak가3이상이면_STREAK_3반환() = runTest {
        val prev = record(7, goal = 8)
        val next = record(8, goal = 8)

        val result = checker.check(prev, next, StreakInfo(currentStreak = 3))

        assertEquals(Achievement.STREAK_3, result)
    }

    @Test
    fun check_목표달성하고streak가7이상이면_STREAK_7반환() = runTest {
        val prev = record(7, goal = 8)
        val next = record(8, goal = 8)

        val result = checker.check(prev, next, StreakInfo(currentStreak = 10))

        assertEquals(Achievement.STREAK_7, result)
    }

    @Test
    fun check_목표달성하고streak가30이상이면_STREAK_30반환() = runTest {
        val prev = record(7, goal = 8)
        val next = record(8, goal = 8)

        val result = checker.check(prev, next, StreakInfo(currentStreak = 30))

        assertEquals(Achievement.STREAK_30, result)
    }

    @Test
    fun check_streak타이틀이미획득했으면_GOAL_ACHIEVED로대체된다() = runTest {
        val prev = record(7, goal = 8)
        val next = record(8, goal = 8)
        coEvery { dataStore.isAlreadyEarned("2026-07-02", Achievement.STREAK_7) } returns true

        val result = checker.check(prev, next, StreakInfo(currentStreak = 7))

        assertEquals(Achievement.GOAL_ACHIEVED, result)
    }

    @Test
    fun check_50퍼센트돌파시점에_HALF_WAY반환() = runTest {
        val prev = record(3, goal = 8)  // 37.5%
        val next = record(4, goal = 8)  // 50%

        val result = checker.check(prev, next, StreakInfo())

        assertEquals(Achievement.HALF_WAY, result)
    }

    @Test
    fun check_이미50퍼센트넘은상태에서더마셔도_HALF_WAY중복반환안함() = runTest {
        val prev = record(4, goal = 8)  // 50%
        val next = record(5, goal = 8)  // 62.5%

        val result = checker.check(prev, next, StreakInfo())

        assertNull(result)
    }

    @Test
    fun check_이미획득한업적은다시반환하지않는다() = runTest {
        coEvery { dataStore.isAlreadyEarned("2026-07-02", Achievement.FIRST_SIP) } returns true

        val result = checker.check(record(0), record(1), StreakInfo())

        assertNull(result)
        coVerify(exactly = 0) { dataStore.markEarned(any(), Achievement.FIRST_SIP) }
    }

    @Test
    fun check_아무조건도해당안되면_null반환() = runTest {
        val prev = record(5, goal = 8)
        val next = record(6, goal = 8)

        val result = checker.check(prev, next, StreakInfo())

        assertNull(result)
    }
}
