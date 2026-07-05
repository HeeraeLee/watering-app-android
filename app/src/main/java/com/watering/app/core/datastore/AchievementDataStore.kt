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
    private val PENDING_DISPLAY_KEY = stringPreferencesKey("pending_display")

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

    // 위젯(AddWaterAction)에서 달성한 업적은 그 자리에서 모달을 띄울 화면이 없으므로,
    // 앱을 다음에 열었을 때 HomeViewModel이 확인해서 보여줄 수 있도록 대기 상태로 저장한다.
    suspend fun setPendingDisplay(achievement: Achievement) {
        context.achievementDataStore.edit { prefs -> prefs[PENDING_DISPLAY_KEY] = achievement.name }
    }

    suspend fun consumePendingDisplay(): Achievement? {
        val prefs = context.achievementDataStore.data.first()
        val name = prefs[PENDING_DISPLAY_KEY] ?: return null
        context.achievementDataStore.edit { it.remove(PENDING_DISPLAY_KEY) }
        return runCatching { Achievement.valueOf(name) }.getOrNull()
    }
}
