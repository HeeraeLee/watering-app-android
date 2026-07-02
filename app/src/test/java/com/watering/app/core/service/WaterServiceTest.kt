package com.watering.app.core.service

import android.content.Context
import com.watering.app.core.data.WaterRepository
import com.watering.app.core.model.DayRecord
import com.watering.app.core.model.DrinkType
import com.watering.app.core.model.StreakInfo
import com.watering.app.widget.WateringWidgetUpdater
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class WaterServiceTest {

    private lateinit var context: Context
    private lateinit var repository: WaterRepository
    private lateinit var widgetUpdater: WateringWidgetUpdater
    private lateinit var service: WaterService

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        repository = mockk(relaxed = true)
        widgetUpdater = mockk(relaxed = true)
        service = WaterService(context, repository, widgetUpdater)
    }

    @Test
    fun addWater_repository에저장하고위젯을갱신한다() = runTest {
        val record = DayRecord(dateKey = "2026-07-02")
        coEvery { repository.addEntry(200, DrinkType.WATER, 8) } returns record

        val result = service.addWater(amount = 200, drinkType = DrinkType.WATER, goal = 8)

        assertEquals(record, result)
        coVerifyOrder {
            repository.addEntry(200, DrinkType.WATER, 8)
            widgetUpdater.updateAll()
        }
    }

    @Test
    fun undoLastEntry_repository에서제거하고위젯을갱신한다() = runTest {
        val record = DayRecord(dateKey = "2026-07-02")
        coEvery { repository.removeLastEntry(8) } returns record

        val result = service.undoLastEntry(goal = 8)

        assertEquals(record, result)
        coVerifyOrder {
            repository.removeLastEntry(8)
            widgetUpdater.updateAll()
        }
    }

    @Test
    fun updateStreak_repository로그대로위임한다() = runTest {
        val record = DayRecord(dateKey = "2026-07-02")
        val current = StreakInfo(currentStreak = 3)
        val updated = current.copy(currentStreak = 4)
        coEvery { repository.updateStreak(record, current, true) } returns updated

        val result = service.updateStreak(record, current, isPremium = true)

        assertEquals(updated, result)
    }

    @Test
    fun resetToday_repository초기화하고위젯을갱신한다() = runTest {
        service.resetToday()

        coVerifyOrder {
            repository.resetToday()
            widgetUpdater.updateAll()
        }
    }

    @Test
    fun clearAllData_repository전체삭제하고위젯을갱신한다() = runTest {
        service.clearAllData()

        coVerifyOrder {
            repository.clearAllData()
            widgetUpdater.updateAll()
        }
    }
}
