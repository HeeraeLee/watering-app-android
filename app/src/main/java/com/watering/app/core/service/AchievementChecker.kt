package com.watering.app.core.service

import com.watering.app.core.datastore.AchievementDataStore
import com.watering.app.core.model.Achievement
import com.watering.app.core.model.DayRecord
import com.watering.app.core.model.StreakInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AchievementChecker @Inject constructor(
    private val dataStore: AchievementDataStore,
    private val analyticsService: AnalyticsService
) {

    suspend fun check(
        prev: DayRecord,
        next: DayRecord,
        streak: StreakInfo
    ): Achievement? {
        val dateKey = next.dateKey

        // 첫 잔 — totalCount는 ml 비례 크레딧(소수)이라 "1잔을 다 채웠다"가 아니라 "처음
        // 기록을 남겼다"를 기준으로 판정해야 함(entries 개수 기반)
        if (prev.entries.isEmpty() && next.entries.size == 1) {
            return emit(dateKey, Achievement.FIRST_SIP)
        }

        // 목표 달성 (이번 잔으로 달성)
        if (!prev.isAchieved && next.isAchieved) {
            // 연속 기록 기반 타이틀 우선 (더 특별함)
            val streakAchievement = when {
                streak.currentStreak >= 365 -> Achievement.STREAK_365
                streak.currentStreak >= 100 -> Achievement.STREAK_100
                streak.currentStreak >= 60  -> Achievement.STREAK_60
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
        val prevRate = prev.achievementRate
        val nextRate = next.achievementRate
        if (prevRate < 0.5 && nextRate >= 0.5 && !next.isAchieved) {
            return emit(dateKey, Achievement.HALF_WAY)
        }

        return null
    }

    private suspend fun emit(dateKey: String, achievement: Achievement): Achievement? {
        if (dataStore.isAlreadyEarned(dateKey, achievement)) return null
        dataStore.markEarned(dateKey, achievement)
        analyticsService.logAchievementUnlocked(achievement.name)
        return achievement
    }
}
