package com.watering.app.features.onboarding

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.watering.app.core.data.SettingsRepository
import com.watering.app.core.model.UserSettings
import com.watering.app.core.service.AnalyticsService
import com.watering.app.core.service.NotificationService
import com.watering.app.ui.theme.WateringTheme
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

// 온보딩은 ViewModel의 상태가 아니라 화면 자체의 rememberSaveable 상태(페이지/목표/컵크기)로
// 진행되므로, 뒤로가기/다음 버튼과 목표 스테퍼·컵 크기 칩 클릭 같은 실제 인터랙션 위주로
// 검증한다. 마지막 페이지의 "시작하기"는 API 33+ 실기기에서 진짜 알림 권한 다이얼로그를 띄워
// 이 테스트로는 제어하기 어려워 범위 밖으로 둔다.
class OnboardingScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun createViewModel(): OnboardingViewModel {
        val settingsRepository = mockk<SettingsRepository> {
            every { userSettings } returns MutableStateFlow(UserSettings())
        }
        val notificationService = mockk<NotificationService>(relaxed = true)
        val analyticsService = mockk<AnalyticsService>(relaxed = true)
        return OnboardingViewModel(settingsRepository, notificationService, analyticsService)
    }

    private fun setContent() {
        composeTestRule.setContent {
            WateringTheme {
                OnboardingScreen(viewModel = createViewModel(), onComplete = {})
            }
        }
    }

    @Test
    fun 웰컴페이지에_타이틀과다음버튼이보인다() {
        setContent()

        composeTestRule.onNodeWithText("워터링에\n오신 것을 환영해요!").assertIsDisplayed()
        composeTestRule.onNodeWithText("다음").assertIsDisplayed()
    }

    @Test
    fun 다음버튼을누르면_목표설정페이지로이동하고기본값1600ml이보인다() {
        setContent()

        composeTestRule.onNodeWithText("다음").performClick()

        composeTestRule.onNodeWithText("목표를 설정해요").assertIsDisplayed()
        composeTestRule.onNodeWithText("1600ml").assertIsDisplayed()
    }

    @Test
    fun 목표증가버튼을누르면_ml값이컵크기만큼증가한다() {
        setContent()
        composeTestRule.onNodeWithText("다음").performClick()

        composeTestRule.onNodeWithContentDescription("목표 증가").performClick()

        // 기본 컵 크기 200ml 기준, 8잔→9잔이면 1800ml
        composeTestRule.onNodeWithText("1800ml").assertIsDisplayed()
    }

    @Test
    fun 컵크기칩을누르면_목표ml이새컵크기기준으로재계산된다() {
        setContent()
        composeTestRule.onNodeWithText("다음").performClick()

        composeTestRule.onNodeWithText("887ml").performClick()

        // 기본 목표 8잔 기준, 컵 크기 887ml로 바꾸면 8×887=7096ml
        composeTestRule.onNodeWithText("7096ml").assertIsDisplayed()
    }

    @Test
    fun 뒤로가기버튼을누르면_웰컴페이지로돌아간다() {
        setContent()
        composeTestRule.onNodeWithText("다음").performClick()
        composeTestRule.onNodeWithText("목표를 설정해요").assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("뒤로").performClick()

        composeTestRule.onNodeWithText("워터링에\n오신 것을 환영해요!").assertIsDisplayed()
    }
}
