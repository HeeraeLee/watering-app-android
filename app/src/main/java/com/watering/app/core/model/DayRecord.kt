package com.watering.app.core.model

import kotlinx.serialization.Serializable

@Serializable
data class DayRecord(
    val dateKey: String,         // "yyyy-MM-dd" 형식
    val entries: List<WaterEntry> = emptyList(),
    val goal: Int = 8            // 목표 잔 수
) {
    val totalCount: Int get() = entries.size
    val achievementRate: Double get() = if (goal == 0) 0.0 else totalCount.toDouble() / goal
    val isAchieved: Boolean get() = totalCount >= goal
}
