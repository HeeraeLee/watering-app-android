package com.watering.app.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.watering.app.core.model.UserSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }

    private object Keys {
        val USER_SETTINGS = stringPreferencesKey("user_settings")
    }

    val userSettings: Flow<UserSettings> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.USER_SETTINGS]
            ?.let { runCatching { json.decodeFromString<UserSettings>(it) }.getOrNull() }
            ?: UserSettings()
    }

    suspend fun updateSettings(settings: UserSettings) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.USER_SETTINGS] = json.encodeToString(settings)
        }
    }

    suspend fun updatePremium(isPremium: Boolean) {
        context.settingsDataStore.edit { prefs ->
            val current = prefs[Keys.USER_SETTINGS]
                ?.let { runCatching { json.decodeFromString<UserSettings>(it) }.getOrNull() }
                ?: UserSettings()
            prefs[Keys.USER_SETTINGS] = json.encodeToString(current.copy(isPremium = isPremium))
        }
    }

    suspend fun completeOnboarding() {
        context.settingsDataStore.edit { prefs ->
            val current = prefs[Keys.USER_SETTINGS]
                ?.let { runCatching { json.decodeFromString<UserSettings>(it) }.getOrNull() }
                ?: UserSettings()
            prefs[Keys.USER_SETTINGS] = json.encodeToString(current.copy(isOnboardingDone = true))
        }
    }
}
