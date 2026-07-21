package com.watering.app.features.settings

import android.app.Activity
import com.watering.app.core.service.ReviewService
import com.watering.app.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class AppInfoViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun requestManualReview_reviewService에활동과함께위임한다() = runTest(mainDispatcherRule.testDispatcher) {
        val reviewService = mockk<ReviewService>(relaxed = true)
        val viewModel = AppInfoViewModel(reviewService)
        val activity = mockk<Activity>()
        coEvery { reviewService.requestManualReview(activity) } returns Unit

        viewModel.requestManualReview(activity)

        coVerify { reviewService.requestManualReview(activity) }
    }
}
