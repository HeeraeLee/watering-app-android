package com.watering.app.core.service

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.google.android.play.core.review.ReviewManagerFactory
import com.watering.app.core.data.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

// 연속 기록 달성(감정적 최고점) 또는 설치 3일 경과 시점에 자동 요청 — 90일 쿨다운 + 평생 최대 3회로 제한한다.
// 설정 화면의 수동 "앱 평가하기"는 이 게이팅과 무관하게 항상 시도하며, 성공/실패와 무관하게 쿨다운을 리셋한다.
@Singleton
class ReviewService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val analyticsService: AnalyticsService,
    private val clock: Clock
) {
    private companion object {
        const val MIN_INSTALL_AGE_MILLIS = 3L * 24 * 60 * 60 * 1000
        const val COOLDOWN_MILLIS = 90L * 24 * 60 * 60 * 1000
        const val MAX_REQUEST_COUNT = 3
    }

    suspend fun requestReviewIfEligible(activity: Activity) {
        if (!isEligible()) return
        settingsRepository.markReviewRequested(clock.millis())
        analyticsService.logReviewFlowRequested(trigger = "auto")
        launchReviewFlow(activity)
    }

    suspend fun requestManualReview(activity: Activity) {
        settingsRepository.markReviewRequested(clock.millis())
        analyticsService.logReviewFlowRequested(trigger = "manual")
        launchReviewFlow(activity, onNotSuccessful = { openPlayStoreListing(activity) })
    }

    private suspend fun isEligible(): Boolean {
        val settings = settingsRepository.userSettings.first()
        if (settings.reviewRequestCount >= MAX_REQUEST_COUNT) return false
        if (clock.millis() - firstInstallTimeMillis() < MIN_INSTALL_AGE_MILLIS) return false
        val last = settings.lastReviewRequestedAtMillis
        if (last != null && clock.millis() - last < COOLDOWN_MILLIS) return false
        return true
    }

    private fun firstInstallTimeMillis(): Long {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        return packageInfo.firstInstallTime
    }

    private fun launchReviewFlow(activity: Activity, onNotSuccessful: (() -> Unit)? = null) {
        val manager = ReviewManagerFactory.create(context)
        manager.requestReviewFlow().addOnCompleteListener { request ->
            if (request.isSuccessful) {
                manager.launchReviewFlow(activity, request.result)
            } else {
                onNotSuccessful?.invoke()
            }
        }
    }

    private fun openPlayStoreListing(activity: Activity) {
        val marketUri = Uri.parse("market://details?id=${context.packageName}")
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, marketUri).setPackage("com.android.vending"))
        } catch (e: ActivityNotFoundException) {
            val webUri = Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
            activity.startActivity(Intent(Intent.ACTION_VIEW, webUri))
        }
    }
}
