package com.watering.app.core.service

import android.app.Activity
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.watering.app.core.data.SettingsRepository
import com.watering.app.core.model.UserSettings
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ReviewServiceTest {

    private lateinit var context: Context
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var analyticsService: AnalyticsService

    private val packageName = "com.watering.app.debug"
    private val now = Instant.parse("2026-07-13T00:00:00Z")
    private val installedAt = now.minusMillis(10L * 24 * 60 * 60 * 1000) // 10일 전 설치 (3일 조건은 충족)

    @Before
    fun setUp() {
        mockkStatic(ReviewManagerFactory::class)
        every { ReviewManagerFactory.create(any()) } returns mockk<ReviewManager>(relaxed = true)

        val packageManager = mockk<PackageManager>()
        every { packageManager.getPackageInfo(packageName, 0) } returns PackageInfo().apply {
            firstInstallTime = installedAt.toEpochMilli()
        }

        context = mockk<Context>()
        every { context.packageName } returns packageName
        every { context.packageManager } returns packageManager
        analyticsService = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkStatic(ReviewManagerFactory::class)
    }

    private fun createService(settings: UserSettings, clock: Clock = Clock.fixed(now, ZoneOffset.UTC)): ReviewService {
        settingsRepository = mockk(relaxed = true) {
            every { userSettings } returns MutableStateFlow(settings)
        }
        return ReviewService(context, settingsRepository, analyticsService, clock)
    }

    @Test
    fun requestReviewIfEligible_설치3일미만이면요청하지않는다() = runTest {
        val recentClock = Clock.fixed(installedAt.plusMillis(24L * 60 * 60 * 1000), ZoneOffset.UTC) // 설치 1일 후
        val service = createService(UserSettings(), clock = recentClock)

        service.requestReviewIfEligible(mockk<Activity>())

        coVerify(exactly = 0) { settingsRepository.markReviewRequested(any()) }
    }

    @Test
    fun requestReviewIfEligible_쿨다운90일이내면요청하지않는다() = runTest {
        val settings = UserSettings(
            lastReviewRequestedAtMillis = now.minusMillis(30L * 24 * 60 * 60 * 1000).toEpochMilli(),
            reviewRequestCount = 1
        )
        val service = createService(settings)

        service.requestReviewIfEligible(mockk<Activity>())

        coVerify(exactly = 0) { settingsRepository.markReviewRequested(any()) }
    }

    @Test
    fun requestReviewIfEligible_최대횟수3회에도달하면쿨다운이지나도요청하지않는다() = runTest {
        val settings = UserSettings(
            lastReviewRequestedAtMillis = now.minusMillis(200L * 24 * 60 * 60 * 1000).toEpochMilli(),
            reviewRequestCount = 3
        )
        val service = createService(settings)

        service.requestReviewIfEligible(mockk<Activity>())

        coVerify(exactly = 0) { settingsRepository.markReviewRequested(any()) }
    }

    @Test
    fun requestReviewIfEligible_모든조건충족하면요청시각을기록한다() = runTest {
        val settings = UserSettings(reviewRequestCount = 0, lastReviewRequestedAtMillis = null)
        val service = createService(settings)

        service.requestReviewIfEligible(mockk<Activity>())

        coVerify { settingsRepository.markReviewRequested(now.toEpochMilli()) }
    }

    @Test
    fun requestManualReview_자동요청게이팅조건을충족하지못해도항상기록을남긴다() = runTest {
        val settings = UserSettings(
            lastReviewRequestedAtMillis = now.toEpochMilli(),
            reviewRequestCount = 3
        )
        val service = createService(settings)

        service.requestManualReview(mockk<Activity>())

        coVerify { settingsRepository.markReviewRequested(now.toEpochMilli()) }
    }
}
