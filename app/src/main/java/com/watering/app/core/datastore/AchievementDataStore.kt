package com.watering.app.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.watering.app.core.model.Achievement
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

private val Context.achievementDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "achievements")

@Singleton
class AchievementDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val KEY = stringPreferencesKey("earned_today")

    // "yyyy-MM-dd|ACHIEVEMENT_NAME" 형식으로 저장, ';' 구분
    private suspend fun getEarnedSet(): Set<String> {
        val prefs = context.achievementDataStore.data.first()
        return prefs[KEY]?.split(";")?.toSet() ?: emptySet()
    }

    suspend fun isAlreadyEarned(dateKey: String, achievement: Achievement): Boolean {
        val token = "$dateKey|${achievement.name}"
        return getEarnedSet().contains(token)
    }

    suspend fun markEarned(dateKey: String, achievement: Achievement) {
        val token = "$dateKey|${achievement.name}"
        context.achievementDataStore.edit { prefs ->
            val current = prefs[KEY]?.split(";")?.toMutableSet() ?: mutableSetOf()
            // 오래된 날짜 항목 정리 (오늘 것만 유지)
            val filtered = current.filter { it.startsWith(dateKey) }.toMutableSet()
            filtered.add(token)
            prefs[KEY] = filtered.joinToString(";")
        }
    }
}
