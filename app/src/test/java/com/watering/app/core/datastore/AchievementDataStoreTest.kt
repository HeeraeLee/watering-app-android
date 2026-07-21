package com.watering.app.core.datastore

import android.content.Context
import com.watering.app.core.model.Achievement
import io.mockk.every
import io.mockk.mockk
import java.nio.file.Files
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// WaterDataStoreTest와 같은 이유로 파일 분리 대신 명시적 리셋으로 격리한다. AchievementDataStore엔
// "오늘 획득" 저장분(KEY)을 통째로 지우는 API가 없어, 날짜 기반 테스트는 테스트마다 서로 겹치지
// 않는 고유 dateKey를 써서 자연히 격리시킨다(markEarned가 같은 날짜 항목만 남기고 필터링하는
// 로직이라, 다른 테스트가 쓴 다른 날짜 토큰과 섞이지 않음). 평생 업적(LIFETIME_KEY)은
// restoreLifetimeEarned(emptySet())로, 대기 중 표시(PENDING_DISPLAY_KEY)는 consumePendingDisplay()
// 선소진으로 각각 명시적 리셋한다.
class AchievementDataStoreTest {

    private lateinit var context: Context
    private lateinit var dataStore: AchievementDataStore

    @Before
    fun setUp() = runTest {
        val tempDir = Files.createTempDirectory("achievementDataStoreTest").toFile()
        context = mockk(relaxed = true)
        every { context.applicationContext } returns context
        every { context.filesDir } returns tempDir
        dataStore = AchievementDataStore(context)
        dataStore.restoreLifetimeEarned(emptySet())
        dataStore.consumePendingDisplay()
    }

    @Test
    fun isAlreadyEarned_비스트릭업적을markEarned하면같은날짜에서true를반환한다() = runTest {
        val dateKey = "2099-01-01"

        dataStore.markEarned(dateKey, Achievement.FIRST_SIP)

        assertTrue(dataStore.isAlreadyEarned(dateKey, Achievement.FIRST_SIP))
    }

    @Test
    fun isAlreadyEarned_markEarned한적없으면false를반환한다() = runTest {
        val dateKey = "2099-01-02"

        assertFalse(dataStore.isAlreadyEarned(dateKey, Achievement.FIRST_SIP))
    }

    @Test
    fun markEarned_같은날짜에다른비스트릭업적을추가로기록해도기존항목이유지된다() = runTest {
        val dateKey = "2099-01-03"

        dataStore.markEarned(dateKey, Achievement.FIRST_SIP)
        dataStore.markEarned(dateKey, Achievement.GOAL_ACHIEVED)

        assertTrue(dataStore.isAlreadyEarned(dateKey, Achievement.FIRST_SIP))
        assertTrue(dataStore.isAlreadyEarned(dateKey, Achievement.GOAL_ACHIEVED))
    }

    @Test
    fun markEarned_스트릭기반업적은dateKey와무관하게평생한번만획득처리된다() = runTest {
        assertFalse(dataStore.isAlreadyEarned("2099-02-01", Achievement.STREAK_7))

        dataStore.markEarned("2099-02-01", Achievement.STREAK_7)

        // isStreakBased 업적은 lifetime 집합만 보므로, 완전히 다른 날짜로 물어봐도 true여야 한다
        assertTrue(dataStore.isAlreadyEarned("2099-02-02", Achievement.STREAK_7))
    }

    @Test
    fun getLifetimeEarnedNames_restoreLifetimeEarned으로저장한이름들을그대로반환한다() = runTest {
        dataStore.restoreLifetimeEarned(setOf("STREAK_3", "STREAK_7"))

        val result = dataStore.getLifetimeEarnedNames()

        assertEquals(setOf("STREAK_3", "STREAK_7"), result)
    }

    @Test
    fun setPendingDisplay_consumePendingDisplay로한번읽으면제거된다() = runTest {
        dataStore.setPendingDisplay(Achievement.HALF_WAY)

        val first = dataStore.consumePendingDisplay()
        val second = dataStore.consumePendingDisplay()

        assertEquals(Achievement.HALF_WAY, first)
        assertNull(second)
    }

    @Test
    fun consumePendingDisplay_대기중인게없으면null을반환한다() = runTest {
        val result = dataStore.consumePendingDisplay()

        assertNull(result)
    }
}
