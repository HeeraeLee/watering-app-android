package com.watering.app.features.settings

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.platform.app.InstrumentationRegistry
import com.watering.app.core.data.SettingsRepository
import com.watering.app.core.model.UserSettings
import com.watering.app.core.service.CsvExportService
import com.watering.app.core.service.HealthConnectAvailability
import com.watering.app.core.service.HealthConnectService
import com.watering.app.core.service.NotificationService
import com.watering.app.core.service.WaterService
import com.watering.app.ui.theme.WateringTheme
import com.watering.app.widget.WateringWidgetUpdater
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun createViewModel(settings: UserSettings = UserSettings()): SettingsViewModel {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val settingsRepository = mockk<SettingsRepository>(relaxed = true) {
            every { userSettings } returns MutableStateFlow(settings)
        }
        val notificationService = mockk<NotificationService>(relaxed = true)
        val waterService = mockk<WaterService>(relaxed = true)
        val widgetUpdater = mockk<WateringWidgetUpdater>(relaxed = true)
        val healthConnectService = mockk<HealthConnectService> {
            every { availability } returns HealthConnectAvailability.AVAILABLE
        }
        val csvExportService = mockk<CsvExportService>(relaxed = true)
        return SettingsViewModel(
            context, settingsRepository, notificationService, waterService,
            widgetUpdater, healthConnectService, csvExportService
        )
    }

    // 화면 전체가 LazyColumn이라 아래쪽 "더보기" 메뉴 행들은 스크롤해야 컴포지션에 나타난다
    // (LazyColumn은 화면 밖 아이템을 아예 안 그림) — 스크롤 가능한 노드를 찾아 그쪽으로 스크롤한다
    private fun scrollToText(text: String) {
        composeTestRule.onNode(hasScrollAction()).performScrollToNode(hasText(text))
    }

    @Test
    fun 더보기메뉴행타이틀이모두보인다() {
        composeTestRule.setContent {
            WateringTheme {
                SettingsScreen(
                    onBack = {}, onNavigateToWidgetTheme = {}, onNavigateToHealthConnect = {},
                    onNavigateToNotifications = {}, onNavigateToBackup = {}, onNavigateToAppInfo = {},
                    viewModel = createViewModel()
                )
            }
        }

        scrollToText("알림 설정")
        composeTestRule.onNodeWithText("알림 설정").assertIsDisplayed()
        scrollToText("건강 연동")
        composeTestRule.onNodeWithText("건강 연동").assertIsDisplayed()
        scrollToText("위젯 테마")
        composeTestRule.onNodeWithText("위젯 테마").assertIsDisplayed()
        scrollToText("백업 및 복원")
        composeTestRule.onNodeWithText("백업 및 복원").assertIsDisplayed()
        scrollToText("앱 정보")
        composeTestRule.onNodeWithText("앱 정보").assertIsDisplayed()
    }

    @Test
    fun 컵크기그룹에스탠리퀜처887ml프리셋이보인다() {
        composeTestRule.setContent {
            WateringTheme {
                SettingsScreen(
                    onBack = {}, onNavigateToWidgetTheme = {}, onNavigateToHealthConnect = {},
                    onNavigateToNotifications = {}, onNavigateToBackup = {}, onNavigateToAppInfo = {},
                    viewModel = createViewModel()
                )
            }
        }

        composeTestRule.onNodeWithText("887ml").assertIsDisplayed()
        composeTestRule.onNodeWithText("스탠리 퀜처").assertIsDisplayed()
    }

    @Test
    fun 알림설정메뉴를누르면_해당콜백이호출된다() {
        var navigated = false
        composeTestRule.setContent {
            WateringTheme {
                SettingsScreen(
                    onBack = {}, onNavigateToWidgetTheme = {}, onNavigateToHealthConnect = {},
                    onNavigateToNotifications = { navigated = true }, onNavigateToBackup = {}, onNavigateToAppInfo = {},
                    viewModel = createViewModel()
                )
            }
        }

        scrollToText("알림 설정")
        composeTestRule.onNodeWithText("알림 설정").performClick()

        assert(navigated)
    }

    @Test
    fun 뒤로가기버튼을누르면_onBack이호출된다() {
        var backPressed = false
        composeTestRule.setContent {
            WateringTheme {
                SettingsScreen(
                    onBack = { backPressed = true }, onNavigateToWidgetTheme = {}, onNavigateToHealthConnect = {},
                    onNavigateToNotifications = {}, onNavigateToBackup = {}, onNavigateToAppInfo = {},
                    viewModel = createViewModel()
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("뒤로").performClick()

        assert(backPressed)
    }
}
