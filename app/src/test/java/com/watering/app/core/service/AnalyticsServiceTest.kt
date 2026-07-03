package com.watering.app.core.service

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

// Bundle은 Android 프레임워크 클래스라 순수 JUnit(Robolectric 미사용) 환경에서 실제 생성자를 호출할 수 없다.
// mockkConstructor로 모든 Bundle 인스턴스의 생성/메서드 호출을 가로채 검증한다.
class AnalyticsServiceTest {

    private lateinit var analytics: FirebaseAnalytics
    private lateinit var service: AnalyticsService

    @Before
    fun setUp() {
        analytics = mockk(relaxed = true)
        service = AnalyticsService(analytics)
        mockkConstructor(Bundle::class)
        every { anyConstructed<Bundle>().putString(any(), any()) } just Runs
        every { anyConstructed<Bundle>().putLong(any(), any()) } just Runs
    }

    @After
    fun tearDown() {
        unmockkConstructor(Bundle::class)
    }

    @Test
    fun logScreenView_screen_view이벤트와화면이름을기록한다() {
        service.logScreenView("home")

        verify { anyConstructed<Bundle>().putString(FirebaseAnalytics.Param.SCREEN_NAME, "home") }
        verify { analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, any()) }
    }

    @Test
    fun logRecordAdd_record_add이벤트와용량드링크타입출처를기록한다() {
        service.logRecordAdd(amountMl = 200, drinkType = "WATER", source = "home")

        verify { anyConstructed<Bundle>().putLong("amount_ml", 200L) }
        verify { anyConstructed<Bundle>().putString("drink_type", "WATER") }
        verify { anyConstructed<Bundle>().putString("source", "home") }
        verify { analytics.logEvent("record_add", any()) }
    }

    @Test
    fun logRecordUndo_record_undo이벤트를기록한다() {
        service.logRecordUndo()

        verify { analytics.logEvent("record_undo", any()) }
    }

    @Test
    fun logAchievementUnlocked_achievement_unlocked이벤트와타입을기록한다() {
        service.logAchievementUnlocked("STREAK_7")

        verify { anyConstructed<Bundle>().putString("achievement_type", "STREAK_7") }
        verify { analytics.logEvent("achievement_unlocked", any()) }
    }

    @Test
    fun logOnboardingComplete_onboarding_complete이벤트와설정값을기록한다() {
        service.logOnboardingComplete(dailyGoal = 8, cupSize = 200, notificationEnabled = true)

        verify { anyConstructed<Bundle>().putLong("daily_goal", 8L) }
        verify { anyConstructed<Bundle>().putLong("cup_size", 200L) }
        verify { anyConstructed<Bundle>().putLong("notification_enabled", 1L) }
        verify { analytics.logEvent("onboarding_complete", any()) }
    }

    @Test
    fun logOnboardingComplete_알림비활성화시0으로기록한다() {
        service.logOnboardingComplete(dailyGoal = 8, cupSize = 200, notificationEnabled = false)

        verify { anyConstructed<Bundle>().putLong("notification_enabled", 0L) }
    }

    @Test
    fun logPurchaseStarted_purchase_started이벤트와상품id를기록한다() {
        service.logPurchaseStarted("com.watering.app.premium.monthly")

        verify { anyConstructed<Bundle>().putString("product_id", "com.watering.app.premium.monthly") }
        verify { analytics.logEvent("purchase_started", any()) }
    }

    @Test
    fun logPurchaseSuccess_purchase_success이벤트와상품id를기록한다() {
        service.logPurchaseSuccess("com.watering.app.premium.yearly")

        verify { anyConstructed<Bundle>().putString("product_id", "com.watering.app.premium.yearly") }
        verify { analytics.logEvent("purchase_success", any()) }
    }

    @Test
    fun logPurchaseCancelled_purchase_cancelled이벤트와상품id를기록한다() {
        service.logPurchaseCancelled("com.watering.app.premium.monthly")

        verify { anyConstructed<Bundle>().putString("product_id", "com.watering.app.premium.monthly") }
        verify { analytics.logEvent("purchase_cancelled", any()) }
    }

    @Test
    fun logPurchaseFailed_purchase_failed이벤트와상품id및에러코드를기록한다() {
        service.logPurchaseFailed("com.watering.app.premium.monthly", "3")

        verify { anyConstructed<Bundle>().putString("product_id", "com.watering.app.premium.monthly") }
        verify { anyConstructed<Bundle>().putString("error_code", "3") }
        verify { analytics.logEvent("purchase_failed", any()) }
    }

    @Test
    fun setPremiumUserProperty_is_premium속성을true문자열로설정한다() {
        service.setPremiumUserProperty(true)

        verify { analytics.setUserProperty("is_premium", "true") }
    }

    @Test
    fun setPremiumUserProperty_is_premium속성을false문자열로설정한다() {
        service.setPremiumUserProperty(false)

        verify { analytics.setUserProperty("is_premium", "false") }
    }
}
