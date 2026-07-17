package com.watering.app.core.model

import kotlinx.serialization.Serializable

// 연간 통계(히트맵)용 경량 일별 집계 — DayRecord와 달리 entries(타임스탬프 상세)는 담지 않는다.
// 365일치를 보관해야 해서 엔트리 전체를 담으면 저장 용량·쓰기 지연이 커지므로 집계값만 유지한다.
@Serializable
data class DailyAchievement(
    val dateKey: String,         // "yyyy-MM-dd" 형식
    val totalCount: Double,      // DayRecord.totalCount(ml 비례 크레딧)를 그대로 복사
    val goal: Int
) {
    val achievementRate: Double get() = if (goal == 0) 0.0 else totalCount / goal
    val isAchieved: Boolean get() = totalCount >= goal
}
