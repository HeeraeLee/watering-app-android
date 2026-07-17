package com.watering.app.core.model

import kotlinx.serialization.Serializable

@Serializable
data class DayRecord(
    val dateKey: String,         // "yyyy-MM-dd" 형식
    val entries: List<WaterEntry> = emptyList(),
    val goal: Int = 8,           // 목표 잔 수
    val cupSize: Int = 200       // 기록 당시 컵 크기(ml) 스냅샷 — goal과 동일한 패턴
) {
    // 기록 개수가 아니라 실제 ml 합 / 컵 크기로 계산 — 200ml 기록이 887ml 컵 목표에서
    // 통째로 1잔 취급되는 걸 막기 위해 ml 비례 크레딧을 쓴다(2026-07-17)
    val totalCount: Double get() =
        if (cupSize <= 0) 0.0 else entries.sumOf { it.amount }.toDouble() / cupSize
    val achievementRate: Double get() = if (goal == 0) 0.0 else totalCount / goal
    val isAchieved: Boolean get() = totalCount >= goal
}
