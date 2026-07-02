package com.watering.app.core.model

import kotlinx.serialization.Serializable

@Serializable
data class UserSettings(
    val dailyGoal: Int = 8,
    val cupSize: Int = 200,              // ml
    val notificationEnabled: Boolean = true,
    val notificationInterval: Int = 120, // 분
    val notificationStart: Int = 8,      // 시
    val notificationEnd: Int = 22,       // 시
    val dustAlertEnabled: Boolean = false,
    val heatAlertEnabled: Boolean = false,
    val healthConnectEnabled: Boolean = false,
    val isPremium: Boolean = false,
    val isOnboardingDone: Boolean = false,
    val reviewRequested: Boolean = false  // 인앱 리뷰 요청 1회 제한용
)
