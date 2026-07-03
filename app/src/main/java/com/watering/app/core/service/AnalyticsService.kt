package com.watering.app.core.service

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject
import javax.inject.Singleton

// 이벤트/파라미터 이름의 단일 소스. Firebase 제약: 이벤트명 40자, 파라미터명 40자, 문자열 파라미터 값 100자 이내.
@Singleton
class AnalyticsService @Inject constructor(
    private val analytics: FirebaseAnalytics
) {

    private companion object {
        const val EVENT_RECORD_ADD = "record_add"
        const val EVENT_RECORD_UNDO = "record_undo"
        const val EVENT_ACHIEVEMENT_UNLOCKED = "achievement_unlocked"
        const val EVENT_ONBOARDING_COMPLETE = "onboarding_complete"
        const val EVENT_PURCHASE_STARTED = "purchase_started"
        const val EVENT_PURCHASE_SUCCESS = "purchase_success"
        const val EVENT_PURCHASE_CANCELLED = "purchase_cancelled"
        const val EVENT_PURCHASE_FAILED = "purchase_failed"

        const val PARAM_AMOUNT_ML = "amount_ml"
        const val PARAM_DRINK_TYPE = "drink_type"
        const val PARAM_SOURCE = "source"
        const val PARAM_ACHIEVEMENT_TYPE = "achievement_type"
        const val PARAM_DAILY_GOAL = "daily_goal"
        const val PARAM_CUP_SIZE = "cup_size"
        const val PARAM_NOTIFICATION_ENABLED = "notification_enabled"
        const val PARAM_PRODUCT_ID = "product_id"
        const val PARAM_ERROR_CODE = "error_code"

        const val USER_PROPERTY_IS_PREMIUM = "is_premium"
    }

    fun logScreenView(screenName: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
        }
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    fun logRecordAdd(amountMl: Int, drinkType: String, source: String) {
        val bundle = Bundle().apply {
            putLong(PARAM_AMOUNT_ML, amountMl.toLong())
            putString(PARAM_DRINK_TYPE, drinkType)
            putString(PARAM_SOURCE, source)
        }
        analytics.logEvent(EVENT_RECORD_ADD, bundle)
    }

    fun logRecordUndo() {
        analytics.logEvent(EVENT_RECORD_UNDO, Bundle())
    }

    fun logAchievementUnlocked(achievementType: String) {
        val bundle = Bundle().apply {
            putString(PARAM_ACHIEVEMENT_TYPE, achievementType)
        }
        analytics.logEvent(EVENT_ACHIEVEMENT_UNLOCKED, bundle)
    }

    fun logOnboardingComplete(dailyGoal: Int, cupSize: Int, notificationEnabled: Boolean) {
        val bundle = Bundle().apply {
            putLong(PARAM_DAILY_GOAL, dailyGoal.toLong())
            putLong(PARAM_CUP_SIZE, cupSize.toLong())
            putLong(PARAM_NOTIFICATION_ENABLED, if (notificationEnabled) 1L else 0L)
        }
        analytics.logEvent(EVENT_ONBOARDING_COMPLETE, bundle)
    }

    fun logPurchaseStarted(productId: String) {
        val bundle = Bundle().apply {
            putString(PARAM_PRODUCT_ID, productId)
        }
        analytics.logEvent(EVENT_PURCHASE_STARTED, bundle)
    }

    fun logPurchaseSuccess(productId: String) {
        val bundle = Bundle().apply {
            putString(PARAM_PRODUCT_ID, productId)
        }
        analytics.logEvent(EVENT_PURCHASE_SUCCESS, bundle)
    }

    fun logPurchaseCancelled(productId: String) {
        val bundle = Bundle().apply {
            putString(PARAM_PRODUCT_ID, productId)
        }
        analytics.logEvent(EVENT_PURCHASE_CANCELLED, bundle)
    }

    fun logPurchaseFailed(productId: String, errorCode: String) {
        val bundle = Bundle().apply {
            putString(PARAM_PRODUCT_ID, productId)
            putString(PARAM_ERROR_CODE, errorCode)
        }
        analytics.logEvent(EVENT_PURCHASE_FAILED, bundle)
    }

    fun setPremiumUserProperty(isPremium: Boolean) {
        analytics.setUserProperty(USER_PROPERTY_IS_PREMIUM, isPremium.toString())
    }
}
