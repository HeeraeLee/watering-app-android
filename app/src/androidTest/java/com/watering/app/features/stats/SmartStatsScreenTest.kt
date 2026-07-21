package com.watering.app.features.stats

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.watering.app.core.data.SettingsRepository
import com.watering.app.core.data.WaterRepository
import com.watering.app.core.model.DayRecord
import com.watering.app.core.model.UserSettings
import com.watering.app.ui.theme.WateringTheme
import io.mockk.every
import io.mockk.mockk
import java.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class SmartStatsScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun createViewModel(): SmartStatsViewModel {
        val waterRepository = mockk<WaterRepository> {
            every { getHistory() } returns MutableStateFlow(emptyMap())
            every { getAnnualHistory() } returns MutableStateFlow(emptyMap())
            every { todayRecord } returns MutableStateFlow(DayRecord(dateKey = "2026-07-21", goal = 8, entries = emptyList()))
        }
        val settingsRepository = mockk<SettingsRepository> {
            every { userSettings } returns MutableStateFlow(UserSettings(dailyGoal = 8, cupSize = 200))
        }
        return SmartStatsViewModel(waterRepository, settingsRepository, Clock.systemDefaultZone())
    }

    @Test
    fun 화면제목과탭이보인다() {
        composeTestRule.setContent {
            WateringTheme {
                SmartStatsScreen(onBack = {}, viewModel = createViewModel())
            }
        }

        composeTestRule.onNodeWithText("스마트 통계").assertIsDisplayed()
        // 탭 라벨과 탭 콘텐츠 안 카드 제목이 같은 문자열이라 화면에 2번씩 나타남
        composeTestRule.onAllNodesWithText("30일 트렌드")[0].assertIsDisplayed()
        composeTestRule.onAllNodesWithText("연간 기록")[0].assertIsDisplayed()
    }

    // 오늘 오전 작업(수분 환산율 고지)의 회귀 확인 — 기록이 없으면 0ml로 표시되고, 고지 캡션이 그대로 보인다
    @Test
    fun 수분섭취량카드에환산율고지캡션이보인다() {
        composeTestRule.setContent {
            WateringTheme {
                SmartStatsScreen(onBack = {}, viewModel = createViewModel())
            }
        }

        composeTestRule.onNodeWithText("오늘 실제 수분 섭취량: 약 0ml").assertIsDisplayed()
        composeTestRule.onNodeWithText("마신 양이 아니라 음료별 수분 환산율을 반영한 값이에요. 목표·연속 기록에는 영향 없어요.")
            .assertIsDisplayed()
    }

    @Test
    fun 뒤로가기버튼을누르면_onBack이호출된다() {
        var backPressed = false
        composeTestRule.setContent {
            WateringTheme {
                SmartStatsScreen(onBack = { backPressed = true }, viewModel = createViewModel())
            }
        }

        composeTestRule.onNodeWithContentDescription("뒤로").performClick()

        assert(backPressed)
    }
}
