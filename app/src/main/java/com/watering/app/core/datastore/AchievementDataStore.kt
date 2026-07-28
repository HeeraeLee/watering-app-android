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

// pending_display는 "위젯 탭 후 다음 앱 실행 1회에 한해 보여줄 알림 큐"일 뿐이라, earned_today/
// earned_lifetime과 달리 앱 삭제 후 재설치 시점엔 유효하지 않아야 한다. 같은 파일에 있으면
// Auto Backup이 이 키까지 통째로 복원해, 재설치 직후 아무 행동도 안 했는데 예전 위젯 탭 때
// 큐잉된 성취 카드가 떠버리는 문제가 있었다(2026-07-28). 그래서 별도 파일로 분리하고
// data_extraction_rules.xml / fullBackupContent.xml에서 이 파일만 백업 제외한다.
private val Context.achievementPendingDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "achievement_pending")

@Singleton
class AchievementDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val KEY = stringPreferencesKey("earned_today")
    private val LIFETIME_KEY = stringPreferencesKey("earned_lifetime")
    private val PENDING_DISPLAY_KEY = stringPreferencesKey("pending_display")

    // "yyyy-MM-dd|ACHIEVEMENT_NAME" 형식으로 저장, ';' 구분
    private suspend fun getEarnedTodaySet(): Set<String> {
        val prefs = context.achievementDataStore.data.first()
        return prefs[KEY]?.split(";")?.toSet() ?: emptySet()
    }

    private suspend fun getLifetimeEarnedSet(): Set<String> {
        val prefs = context.achievementDataStore.data.first()
        return prefs[LIFETIME_KEY]?.split(";")?.toSet() ?: emptySet()
    }

    // 연속 기록 마일스톤(isStreakBased)은 평생 한 번만 — 스트릭이 유지되는 동안 매일 조건을
    // 다시 만족하므로 날짜 기준으로 지우면 매일 재발생한다. 그 외 성취(첫 잔/절반 돌파/오늘 목표
    // 달성)는 매일 리셋되는 게 맞는 동작이라 기존 날짜별 저장을 유지한다
    suspend fun isAlreadyEarned(dateKey: String, achievement: Achievement): Boolean {
        if (achievement.isStreakBased) {
            return getLifetimeEarnedSet().contains(achievement.name)
        }
        val token = "$dateKey|${achievement.name}"
        return getEarnedTodaySet().contains(token)
    }

    suspend fun markEarned(dateKey: String, achievement: Achievement) {
        if (achievement.isStreakBased) {
            context.achievementDataStore.edit { prefs ->
                val current = prefs[LIFETIME_KEY]?.split(";")?.toMutableSet() ?: mutableSetOf()
                current.add(achievement.name)
                prefs[LIFETIME_KEY] = current.joinToString(";")
            }
            return
        }
        val token = "$dateKey|${achievement.name}"
        context.achievementDataStore.edit { prefs ->
            val current = prefs[KEY]?.split(";")?.toMutableSet() ?: mutableSetOf()
            // 오래된 날짜 항목 정리 (오늘 것만 유지)
            val filtered = current.filter { it.startsWith(dateKey) }.toMutableSet()
            filtered.add(token)
            prefs[KEY] = filtered.joinToString(";")
        }
    }

    // 백업/복원 전용 — 평생 업적(연속 기록 마일스톤)은 별도 DataStore("achievements")에 있어
    // WaterDataStore.restoreAll의 백업 스냅샷에 포함되지 않았었음(2026-07-16 수정)
    suspend fun getLifetimeEarnedNames(): Set<String> = getLifetimeEarnedSet()

    suspend fun restoreLifetimeEarned(names: Set<String>) {
        context.achievementDataStore.edit { prefs ->
            prefs[LIFETIME_KEY] = names.joinToString(";")
        }
    }

    // 위젯(AddWaterAction)에서 달성한 업적은 그 자리에서 모달을 띄울 화면이 없으므로,
    // 앱을 다음에 열었을 때 HomeViewModel이 확인해서 보여줄 수 있도록 대기 상태로 저장한다.
    suspend fun setPendingDisplay(achievement: Achievement) {
        context.achievementPendingDataStore.edit { prefs -> prefs[PENDING_DISPLAY_KEY] = achievement.name }
    }

    suspend fun consumePendingDisplay(): Achievement? {
        val prefs = context.achievementPendingDataStore.data.first()
        val name = prefs[PENDING_DISPLAY_KEY] ?: return null
        context.achievementPendingDataStore.edit { it.remove(PENDING_DISPLAY_KEY) }
        return runCatching { Achievement.valueOf(name) }.getOrNull()
    }
}
