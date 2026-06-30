package com.watering.app.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.watering.app.core.model.DayRecord
import com.watering.app.core.model.StreakInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

private val Context.waterDataStore: DataStore<Preferences> by preferencesDataStore(name = "water")

@Singleton
class WaterDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private object Keys {
        val TODAY_RECORD = stringPreferencesKey("today_record")
        val STREAK_INFO = stringPreferencesKey("streak_info")
        val HISTORY = stringPreferencesKey("record_history")   // JSON map
        val LAST_UPDATED = stringPreferencesKey("last_updated")
    }

    val todayRecord: Flow<DayRecord> = context.waterDataStore.data.map { prefs ->
        val todayKey = LocalDate.now().format(formatter)
        prefs[Keys.TODAY_RECORD]
            ?.let { runCatching { json.decodeFromString<DayRecord>(it) }.getOrNull() }
            ?.takeIf { it.dateKey == todayKey }
            ?: DayRecord(dateKey = todayKey)
    }

    val streakInfo: Flow<StreakInfo> = context.waterDataStore.data.map { prefs ->
        prefs[Keys.STREAK_INFO]
            ?.let { runCatching { json.decodeFromString<StreakInfo>(it) }.getOrNull() }
            ?: StreakInfo()
    }

    suspend fun saveTodayRecord(record: DayRecord) {
        context.waterDataStore.edit { prefs ->
            prefs[Keys.TODAY_RECORD] = json.encodeToString(record)
            prefs[Keys.LAST_UPDATED] = System.currentTimeMillis().toString()
        }
        archiveTodayToHistory(record)
    }

    suspend fun saveStreakInfo(streak: StreakInfo) {
        context.waterDataStore.edit { prefs ->
            prefs[Keys.STREAK_INFO] = json.encodeToString(streak)
        }
    }

    fun getHistory(): Flow<Map<String, DayRecord>> = context.waterDataStore.data.map { prefs ->
        prefs[Keys.HISTORY]
            ?.let { runCatching { json.decodeFromString<Map<String, DayRecord>>(it) }.getOrNull() }
            ?: emptyMap()
    }

    private suspend fun archiveTodayToHistory(record: DayRecord) {
        context.waterDataStore.edit { prefs ->
            val existing = prefs[Keys.HISTORY]
                ?.let { runCatching { json.decodeFromString<Map<String, DayRecord>>(it) }.getOrNull() }
                ?: emptyMap()
            val updated = (existing + (record.dateKey to record))
                .entries.sortedByDescending { it.key }.take(90)
                .associate { it.key to it.value }
            prefs[Keys.HISTORY] = json.encodeToString(updated)
        }
    }

    suspend fun resetTodayRecord() {
        val todayKey = LocalDate.now().format(formatter)
        context.waterDataStore.edit { prefs ->
            prefs[Keys.TODAY_RECORD] = json.encodeToString(DayRecord(dateKey = todayKey))
        }
    }

    suspend fun clearAllData() {
        context.waterDataStore.edit { it.clear() }
    }
}
