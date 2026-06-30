package com.watering.app.core.model

enum class Achievement(
    val emoji: String,
    val title: String,
    val message: String,
    val isStreakBased: Boolean = false
) {
    FIRST_SIP("🌱", "시작의 한 모금", "오늘의 첫 물을 마셨어요!"),
    HALF_WAY("💧", "반환점 돌파!", "목표의 절반을 채웠어요"),
    GOAL_ACHIEVED("🏆", "오늘 하루 완벽했어요", "오늘 목표를 달성했어요!"),
    STREAK_3("🔥", "3일 연속 달성!", "3일 연속으로 목표를 달성했어요", isStreakBased = true),
    STREAK_7("⚡", "일주일 챔피언", "7일 연속으로 목표를 달성했어요", isStreakBased = true),
    STREAK_30("👑", "한 달 레전드", "30일 연속으로 목표를 달성했어요", isStreakBased = true)
}
