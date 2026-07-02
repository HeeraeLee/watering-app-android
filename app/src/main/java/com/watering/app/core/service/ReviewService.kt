package com.watering.app.core.service

import android.app.Activity
import android.content.Context
import com.google.android.play.core.review.ReviewManagerFactory
import com.watering.app.core.data.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

// 7일 연속 달성 직후(감정적 최고점)에만 1회 요청 — 이후에는 재요청하지 않는다
@Singleton
class ReviewService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    suspend fun requestReviewIfEligible(activity: Activity) {
        if (settingsRepository.userSettings.first().reviewRequested) return
        settingsRepository.markReviewRequested()

        val manager = ReviewManagerFactory.create(context)
        manager.requestReviewFlow().addOnCompleteListener { request ->
            if (request.isSuccessful) {
                manager.launchReviewFlow(activity, request.result)
            }
        }
    }
}
