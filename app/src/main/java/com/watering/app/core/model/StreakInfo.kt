package com.watering.app.core.model

import kotlinx.serialization.Serializable

@Serializable
data class StreakInfo(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastAchievedDateKey: String = "",  // "yyyy-MM-dd"
    val protectionUsedDates: List<String> = emptyList()  // 보호권을 소비한 날짜(dateKey)들 — 월별 한도 판정과 정확한 롤백 대조에 사용
)
