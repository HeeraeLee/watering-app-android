package com.watering.app.core.service

import android.content.Context
import com.watering.app.core.data.SettingsRepository
import com.watering.app.core.data.WaterRepository
import com.watering.app.core.model.DayRecord
import com.watering.app.core.model.DrinkType
import com.watering.app.core.model.StreakInfo
import com.watering.app.core.model.UserSettings
import com.watering.app.core.model.WaterEntry
import com.watering.app.core.model.WaterUpdateResult
import com.watering.app.widget.WateringWidgetUpdater
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class WaterServiceTest {

    private lateinit var context: Context
    private lateinit var repository: WaterRepository
    private lateinit var widgetUpdater: WateringWidgetUpdater
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var healthConnectService: HealthConnectService
    private lateinit var service: WaterService

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        repository = mockk(relaxed = true)
        widgetUpdater = mockk(relaxed = true)
        settingsRepository = mockk {
            every { userSettings } returns MutableStateFlow(UserSettings(healthConnectEnabled = false))
        }
        healthConnectService = mockk(relaxed = true)
        service = WaterService(context, repository, widgetUpdater, settingsRepository, healthConnectService)
    }

    private fun recordWithEntry(amount: Int = 200, drinkType: DrinkType = DrinkType.WATER, timestamp: Long = 1000L) =
        DayRecord(
            dateKey = "2026-07-02",
            entries = listOf(WaterEntry(timestampMillis = timestamp, amount = amount, drinkType = drinkType))
        )

    private val emptyPrev = DayRecord(dateKey = "2026-07-02")

    @Test
    fun addWater_repository에저장하고위젯을갱신한다() = runTest {
        val record = recordWithEntry()
        coEvery { repository.addEntry(200, DrinkType.WATER, 8) } returns WaterUpdateResult(emptyPrev, record)

        val result = service.addWater(amount = 200, drinkType = DrinkType.WATER, goal = 8)

        assertEquals(record, result.updated)
        coVerifyOrder {
            repository.addEntry(200, DrinkType.WATER, 8)
            widgetUpdater.updateAll()
        }
    }

    @Test
    fun addWater_연동꺼져있으면HealthConnect에쓰지않는다() = runTest {
        val record = recordWithEntry()
        coEvery { repository.addEntry(any(), any(), any()) } returns WaterUpdateResult(emptyPrev, record)

        service.addWater(amount = 200, drinkType = DrinkType.WATER, goal = 8)

        coVerify(exactly = 0) { healthConnectService.writeHydrationRecord(any(), any()) }
    }

    @Test
    fun addWater_연동켜져있고권한있으면환산된수분량으로HealthConnect에쓴다() = runTest {
        every { settingsRepository.userSettings } returns MutableStateFlow(UserSettings(healthConnectEnabled = true))
        coEvery { healthConnectService.hasPermissions(HealthConnectService.HYDRATION_PERMISSIONS) } returns true
        // 커피 200ml * 0.7 = 140ml
        val record = recordWithEntry(amount = 200, drinkType = DrinkType.COFFEE, timestamp = 12345L)
        coEvery { repository.addEntry(200, DrinkType.COFFEE, 8) } returns WaterUpdateResult(emptyPrev, record)

        service.addWater(amount = 200, drinkType = DrinkType.COFFEE, goal = 8)

        coVerify { healthConnectService.writeHydrationRecord(volumeMl = 140.0, timestampMillis = 12345L) }
    }

    @Test
    fun addWater_연동켜져있어도권한없으면HealthConnect에쓰지않는다() = runTest {
        every { settingsRepository.userSettings } returns MutableStateFlow(UserSettings(healthConnectEnabled = true))
        coEvery { healthConnectService.hasPermissions(HealthConnectService.HYDRATION_PERMISSIONS) } returns false
        val record = recordWithEntry()
        coEvery { repository.addEntry(any(), any(), any()) } returns WaterUpdateResult(emptyPrev, record)

        service.addWater(amount = 200, drinkType = DrinkType.WATER, goal = 8)

        coVerify(exactly = 0) { healthConnectService.writeHydrationRecord(any(), any()) }
    }

    @Test
    fun addWater_HealthConnect쓰기가실패해도기록결과는정상반환한다() = runTest {
        every { settingsRepository.userSettings } returns MutableStateFlow(UserSettings(healthConnectEnabled = true))
        coEvery { healthConnectService.hasPermissions(HealthConnectService.HYDRATION_PERMISSIONS) } returns true
        coEvery { healthConnectService.writeHydrationRecord(any(), any()) } throws RuntimeException("boom")
        val record = recordWithEntry()
        coEvery { repository.addEntry(any(), any(), any()) } returns WaterUpdateResult(emptyPrev, record)

        val result = service.addWater(amount = 200, drinkType = DrinkType.WATER, goal = 8)

        assertEquals(record, result.updated)
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
        coEvery { repository.updateStreak(record, current) } returns updated

        val result = service.updateStreak(record, current)

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
