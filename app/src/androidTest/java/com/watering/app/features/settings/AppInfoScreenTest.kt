package com.watering.app.features.settings

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.watering.app.BuildConfig
import com.watering.app.core.service.ReviewService
import com.watering.app.ui.theme.WateringTheme
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

class AppInfoScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun 화면제목과소개문구링크들이보인다() {
        composeTestRule.setContent {
            WateringTheme {
                AppInfoScreen(onBack = {}, viewModel = AppInfoViewModel(mockk(relaxed = true)))
            }
        }

        composeTestRule.onNodeWithText("앱 정보").assertIsDisplayed()
        composeTestRule.onNodeWithText("개인정보처리방침").assertIsDisplayed()
        composeTestRule.onNodeWithText("의견 보내기").assertIsDisplayed()
        composeTestRule.onNodeWithText("앱 평가하기").assertIsDisplayed()
        composeTestRule.onNodeWithText("버전 ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})").assertIsDisplayed()
    }

    @Test
    fun 앱평가하기버튼을누르면_reviewService에수동리뷰요청이위임된다() {
        val reviewService = mockk<ReviewService>(relaxed = true)
        coEvery { reviewService.requestManualReview(any()) } returns Unit
        composeTestRule.setContent {
            WateringTheme {
                AppInfoScreen(onBack = {}, viewModel = AppInfoViewModel(reviewService))
            }
        }

        composeTestRule.onNodeWithText("앱 평가하기").performClick()

        coVerify { reviewService.requestManualReview(any()) }
    }

    @Test
    fun 뒤로가기버튼을누르면_onBack이호출된다() {
        var backPressed = false
        composeTestRule.setContent {
            WateringTheme {
                AppInfoScreen(onBack = { backPressed = true }, viewModel = AppInfoViewModel(mockk(relaxed = true)))
            }
        }

        composeTestRule.onNodeWithContentDescription("뒤로").performClick()

        assert(backPressed)
    }
}
