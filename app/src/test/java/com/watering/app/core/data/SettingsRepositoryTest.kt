package com.watering.app.core.data

import com.watering.app.core.datastore.SettingsDataStore
import com.watering.app.core.model.UserSettings
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SettingsRepositoryTest {

    private lateinit var dataStore: SettingsDataStore
    private lateinit var repository: SettingsRepository

    @Before
    fun setUp() {
        dataStore = mockk(relaxed = true)
        repository = SettingsRepository(dataStore)
    }

    @Test
    fun userSettings_dataStore의flow를그대로노출한다() = runTest {
        val settings = UserSettings(dailyGoal = 10)
        every { dataStore.userSettings } returns flowOf(settings)
        repository = SettingsRepository(dataStore)

        assertEquals(settings, repository.userSettings.first())
    }

    @Test
    fun updateSettings_dataStore에위임한다() = runTest {
        val settings = UserSettings(dailyGoal = 12)
        coEvery { dataStore.updateSettings(settings) } returns Unit

        repository.updateSettings(settings)

        coVerify { dataStore.updateSettings(settings) }
    }

    @Test
    fun updatePremium_dataStore에위임한다() = runTest {
        coEvery { dataStore.updatePremium(true) } returns Unit

        repository.updatePremium(true)

        coVerify { dataStore.updatePremium(true) }
    }

    @Test
    fun completeOnboarding_dataStore에위임한다() = runTest {
        coEvery { dataStore.completeOnboarding() } returns Unit

        repository.completeOnboarding()

        coVerify { dataStore.completeOnboarding() }
    }

    @Test
    fun markReviewRequested_dataStore에위임한다() = runTest {
        coEvery { dataStore.markReviewRequested() } returns Unit

        repository.markReviewRequested()

        coVerify { dataStore.markReviewRequested() }
    }
}
