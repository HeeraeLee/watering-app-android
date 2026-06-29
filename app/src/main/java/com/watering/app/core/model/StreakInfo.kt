package com.watering.app.core.model

import kotlinx.serialization.Serializable

@Serializable
data class StreakInfo(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastAchievedDateKey: String = "",  // "yyyy-MM-dd"
    val protectionUsedThisMonth: Boolean = false,
    val protectionUsedMonthKey: String = ""  // "yyyy-MM"
)
