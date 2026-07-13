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
    val weightKg: Double? = null,          // 체중 기반 목표 계산용, 사용자가 직접 입력
    val isOnboardingDone: Boolean = false,
    val lastReviewRequestedAtMillis: Long? = null, // 인앱 리뷰 마지막 요청 시각(쿨다운 기준)
    val reviewRequestCount: Int = 0,               // 인앱 리뷰 요청 누적 횟수(평생 최대치 제한용)
    val widgetTheme: WidgetTheme = WidgetTheme.DEFAULT
)
