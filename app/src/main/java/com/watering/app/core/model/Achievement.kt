package com.watering.app.core.model

import androidx.annotation.StringRes
import com.watering.app.R

enum class Achievement(
    val emoji: String,
    @StringRes val titleRes: Int,
    @StringRes val messageRes: Int,
    val isStreakBased: Boolean = false,
    val isPremiumOnly: Boolean = false
) {
    FIRST_SIP("🌱", R.string.achievement_first_sip_title, R.string.achievement_first_sip_message),
    HALF_WAY("💧", R.string.achievement_half_way_title, R.string.achievement_half_way_message),
    GOAL_ACHIEVED("🏆", R.string.achievement_goal_achieved_title, R.string.achievement_goal_achieved_message),
    STREAK_3("🔥", R.string.achievement_streak_3_title, R.string.achievement_streak_3_message, isStreakBased = true),
    STREAK_7("⚡", R.string.achievement_streak_7_title, R.string.achievement_streak_7_message, isStreakBased = true),
    STREAK_30("👑", R.string.achievement_streak_30_title, R.string.achievement_streak_30_message, isStreakBased = true),
    STREAK_60("💎", R.string.achievement_streak_60_title, R.string.achievement_streak_60_message, isStreakBased = true, isPremiumOnly = true),
    STREAK_100("🌟", R.string.achievement_streak_100_title, R.string.achievement_streak_100_message, isStreakBased = true, isPremiumOnly = true),
    STREAK_365("🎇", R.string.achievement_streak_365_title, R.string.achievement_streak_365_message, isStreakBased = true, isPremiumOnly = true)
}
