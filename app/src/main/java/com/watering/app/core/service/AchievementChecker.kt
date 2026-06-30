package com.watering.app.core.service

import com.watering.app.core.datastore.AchievementDataStore
import com.watering.app.core.model.Achievement
import com.watering.app.core.model.DayRecord
import com.watering.app.core.model.StreakInfo
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AchievementChecker @Inject constructor(
    private val dataStore: AchievementDataStore
) {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    suspend fun check(
        prev: DayRecord,
        next: DayRecord,
        streak: StreakInfo
    ): Achievement? {
        val dateKey = LocalDate.now().format(formatter)

        // 첫 잔
        if (prev.totalCount == 0 && next.totalCount == 1) {
            return emit(dateKey, Achievement.FIRST_SIP)
        }

        // 목표 달성 (이번 잔으로 달성)
        if (!prev.isAchieved && next.isAchieved) {
            // 스트릭 기반 타이틀 우선 (더 특별함)
            val streakAchievement = when {
                streak.currentStreak >= 30 -> Achievement.STREAK_30
                streak.currentStreak >= 7  -> Achievement.STREAK_7
                streak.currentStreak >= 3  -> Achievement.STREAK_3
                else -> null
            }
            if (streakAchievement != null) {
                val earned = emit(dateKey, streakAchievement)
                if (earned != null) return earned
            }
            return emit(dateKey, Achievement.GOAL_ACHIEVED)
        }

        // 50% 돌파
        val prevRate = if (prev.goal > 0) prev.totalCount.toDouble() / prev.goal else 0.0
        val nextRate = if (next.goal > 0) next.totalCount.toDouble() / next.goal else 0.0
        if (prevRate < 0.5 && nextRate >= 0.5 && !next.isAchieved) {
            return emit(dateKey, Achievement.HALF_WAY)
        }

        return null
    }

    private suspend fun emit(dateKey: String, achievement: Achievement): Achievement? {
        if (dataStore.isAlreadyEarned(dateKey, achievement)) return null
        dataStore.markEarned(dateKey, achievement)
        return achievement
    }
}
