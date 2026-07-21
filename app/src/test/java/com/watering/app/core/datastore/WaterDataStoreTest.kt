package com.watering.app.core.datastore

import android.content.Context
import com.watering.app.core.model.DailyAchievement
import com.watering.app.core.model.DayRecord
import com.watering.app.core.model.DrinkType
import com.watering.app.core.model.StreakInfo
import io.mockk.every
import io.mockk.mockk
import java.nio.file.Files
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// `Context.waterDataStore`(preferencesDataStore 델리게이트)는 프로세스 전체에서 단 하나의
// DataStore<Preferences> 인스턴스만 최초 1회 생성해 캐싱한다 — 어떤 Context 인스턴스로 호출하든
// 이후엔 전부 그 최초 인스턴스를 그대로 재사용한다. 그래서 테스트마다 별도 임시 폴더/Context를
// 새로 만들어도 실제로는 전부 같은 파일을 공유하게 된다. 테스트 간 격리는 파일 분리가 아니라
// 매 테스트 시작 시 clearAllData()로 명시적으로 리셋해서 확보한다(freshDataStore() 참고).
class WaterDataStoreTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        val tempDir = Files.createTempDirectory("waterDataStoreTest").toFile()
        context = mockk(relaxed = true)
        every { context.applicationContext } returns context
        every { context.filesDir } returns tempDir
    }

    private fun dataStore(instant: Instant = FIXED_INSTANT): WaterDataStore =
        WaterDataStore(context, Clock.fixed(instant, ZoneOffset.UTC))

    private suspend fun freshDataStore(instant: Instant = FIXED_INSTANT): WaterDataStore =
        dataStore(instant).also { it.clearAllData() }

    private fun todayKeyFor(instant: Instant): String = LocalDate.ofInstant(instant, ZoneOffset.UTC).toString()

    companion object {
        private val FIXED_INSTANT = Instant.parse("2026-07-21T09:00:00Z")
        private val YESTERDAY_INSTANT = FIXED_INSTANT.minusMillis(24L * 60 * 60 * 1000)
    }

    @Test
    fun addEntry_엔트리추가시todayRecord에반영되고prevUpdated가정확하다() = runTest {
        val store = freshDataStore()

        val result = store.addEntry(amount = 200, drinkType = DrinkType.WATER, goal = 8, cupSize = 200)

        assertTrue(result.prev.entries.isEmpty())
        assertEquals(1, result.updated.entries.size)
        assertEquals(200, result.updated.entries.first().amount)
        assertEquals(DrinkType.WATER, result.updated.entries.first().drinkType)

        val today = store.todayRecord.first()
        assertEquals(1, today.entries.size)
        assertEquals(8, today.goal)
        assertEquals(200, today.cupSize)
    }

    @Test
    fun addEntry_저장된레코드가어제날짜면오늘새레코드로교체된다() = runTest {
        val storeYesterday = freshDataStore(YESTERDAY_INSTANT)
        storeYesterday.addEntry(amount = 200, drinkType = DrinkType.WATER, goal = 8, cupSize = 200)

        val today = dataStore(FIXED_INSTANT).todayRecord.first()

        assertTrue(today.entries.isEmpty())
        assertEquals(todayKeyFor(FIXED_INSTANT), today.dateKey)
    }

    @Test
    fun addEntry_history와annualHistory에자동아카이빙된다() = runTest {
        val store = freshDataStore()

        store.addEntry(amount = 300, drinkType = DrinkType.WATER, goal = 8, cupSize = 200)

        val todayKey = todayKeyFor(FIXED_INSTANT)
        val history = store.getHistory().first()
        val annual = store.getAnnualHistory().first()
        assertEquals(1, history[todayKey]?.entries?.size)
        assertEquals(300.0 / 200, annual[todayKey]?.totalCount)
        assertEquals(8, annual[todayKey]?.goal)
    }

    @Test
    fun removeLastEntry_마지막엔트리가제거되고history도재아카이빙된다() = runTest {
        val store = freshDataStore()
        store.addEntry(amount = 200, drinkType = DrinkType.WATER, goal = 8, cupSize = 200)
        store.addEntry(amount = 300, drinkType = DrinkType.COFFEE, goal = 8, cupSize = 200)

        val updated = store.removeLastEntry(goal = 8, cupSize = 200)

        assertEquals(1, updated.entries.size)
        assertEquals(200, updated.entries.first().amount)
        val todayKey = todayKeyFor(FIXED_INSTANT)
        assertEquals(1, store.getHistory().first()[todayKey]?.entries?.size)
    }

    @Test
    fun saveStreakInfo_저장한값이streakInfo플로우에그대로반영된다() = runTest {
        val store = freshDataStore()
        val streak = StreakInfo(currentStreak = 4, longestStreak = 9, lastAchievedDateKey = "2026-07-20")

        store.saveStreakInfo(streak)

        assertEquals(streak, store.streakInfo.first())
    }

    @Test
    fun resetTodayRecord_goal과cupSize를반영한빈레코드로초기화되고재아카이빙된다() = runTest {
        val store = freshDataStore()
        store.addEntry(amount = 887, drinkType = DrinkType.WATER, goal = 2, cupSize = 887)

        val reset = store.resetTodayRecord(goal = 8, cupSize = 200)

        assertTrue(reset.entries.isEmpty())
        assertEquals(8, reset.goal)
        assertEquals(200, reset.cupSize)
        val todayKey = todayKeyFor(FIXED_INSTANT)
        assertTrue(store.getHistory().first()[todayKey]?.entries.isNullOrEmpty())
    }

    @Test
    fun resetTodayRecordIfStale_이미오늘날짜면아무것도안하고null을반환한다() = runTest {
        val store = freshDataStore()
        store.addEntry(amount = 200, drinkType = DrinkType.WATER, goal = 8, cupSize = 200)

        val result = store.resetTodayRecordIfStale(goal = 8, cupSize = 200)

        assertNull(result)
        assertEquals(1, store.todayRecord.first().entries.size)
    }

    @Test
    fun resetTodayRecordIfStale_어제날짜에머물러있으면초기화한다() = runTest {
        val storeYesterday = freshDataStore(YESTERDAY_INSTANT)
        storeYesterday.addEntry(amount = 200, drinkType = DrinkType.WATER, goal = 8, cupSize = 200)
        val storeToday = dataStore(FIXED_INSTANT)

        val result = storeToday.resetTodayRecordIfStale(goal = 8, cupSize = 200)

        assertTrue(result != null && result.entries.isEmpty())
        assertTrue(storeToday.todayRecord.first().entries.isEmpty())
    }

    @Test
    fun clearAllData_모든데이터가삭제된다() = runTest {
        val store = freshDataStore()
        store.addEntry(amount = 200, drinkType = DrinkType.WATER, goal = 8, cupSize = 200)
        store.saveStreakInfo(StreakInfo(currentStreak = 3))

        store.clearAllData()

        assertTrue(store.todayRecord.first().entries.isEmpty())
        assertEquals(StreakInfo(), store.streakInfo.first())
        assertTrue(store.getHistory().first().isEmpty())
    }

    @Test
    fun restoreAll_백업스냅샷값으로전체를덮어쓴다() = runTest {
        val store = freshDataStore()
        val todayKey = todayKeyFor(FIXED_INSTANT)
        val restoredToday = DayRecord(dateKey = todayKey, goal = 6, cupSize = 300)
        val restoredStreak = StreakInfo(currentStreak = 12, longestStreak = 20)
        val restoredHistory = mapOf(todayKey to restoredToday)
        val restoredAnnual = mapOf(todayKey to DailyAchievement(dateKey = todayKey, totalCount = 5.0, goal = 6))

        store.restoreAll(restoredToday, restoredStreak, restoredHistory, restoredAnnual)

        assertEquals(restoredToday, store.todayRecord.first())
        assertEquals(restoredStreak, store.streakInfo.first())
        assertEquals(restoredHistory, store.getHistory().first())
        assertEquals(restoredAnnual, store.getAnnualHistory().first())
    }
}
