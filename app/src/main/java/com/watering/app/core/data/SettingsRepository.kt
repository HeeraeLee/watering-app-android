package com.watering.app.core.data

import com.watering.app.core.model.UserSettings
import com.watering.app.core.datastore.SettingsDataStore
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: SettingsDataStore
) {
    val userSettings: Flow<UserSettings> = dataStore.userSettings

    suspend fun updateSettings(settings: UserSettings) = dataStore.updateSettings(settings)
    suspend fun updatePremium(isPremium: Boolean) = dataStore.updatePremium(isPremium)
    suspend fun completeOnboarding() = dataStore.completeOnboarding()
}
