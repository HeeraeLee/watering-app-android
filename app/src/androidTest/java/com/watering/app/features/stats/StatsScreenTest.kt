package com.watering.app.features.stats

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.watering.app.core.data.SettingsRepository
import com.watering.app.core.data.WaterRepository
import com.watering.app.core.model.DayRecord
import com.watering.app.core.model.StreakInfo
import com.watering.app.core.model.UserSettings
import com.watering.app.ui.theme.WateringTheme
import io.mockk.every
import io.mockk.mockk
import java.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class StatsScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun createViewModel(
        streak: StreakInfo = StreakInfo(currentStreak = 4, longestStreak = 15)
    ): StatsViewModel {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val waterRepository = mockk<WaterRepository>(relaxed = true) {
            every { getHistory() } returns MutableStateFlow(emptyMap())
            every { todayRecord } returns MutableStateFlow(DayRecord(dateKey = "2026-07-21", goal = 8))
            every { streakInfo } returns MutableStateFlow(streak)
        }
        val settingsRepository = mockk<SettingsRepository> {
            every { userSettings } returns MutableStateFlow(UserSettings(dailyGoal = 8, cupSize = 200))
        }
        return StatsViewModel(context, waterRepository, settingsRepository, Clock.systemDefaultZone())
    }

    @Test
    fun 통계화면제목과섹션헤더가보인다() {
        composeTestRule.setContent {
            WateringTheme {
                StatsScreen(onBack = {}, viewModel = createViewModel())
            }
        }

        composeTestRule.onNodeWithText("통계").assertIsDisplayed()
        composeTestRule.onNodeWithText("이번 주 기록").assertIsDisplayed()
        composeTestRule.onNodeWithText("연속 기록").assertIsDisplayed()
    }

    @Test
    fun 연속기록섹션에currentStreak과longestStreak값이표시된다() {
        composeTestRule.setContent {
            WateringTheme {
                StatsScreen(onBack = {}, viewModel = createViewModel(StreakInfo(currentStreak = 4, longestStreak = 15)))
            }
        }

        composeTestRule.onNodeWithText("4일").assertIsDisplayed()
        composeTestRule.onNodeWithText("15일").assertIsDisplayed()
    }

    @Test
    fun 기록이없으면_주간평균과주간합계가0으로표시된다() {
        composeTestRule.setContent {
            WateringTheme {
                StatsScreen(onBack = {}, viewModel = createViewModel())
            }
        }

        composeTestRule.onNodeWithText("0.0잔").assertIsDisplayed()
        composeTestRule.onNodeWithText("0잔").assertIsDisplayed()
        composeTestRule.onNodeWithText("0 / 7일").assertIsDisplayed()
    }

    @Test
    fun 뒤로가기버튼을누르면_onBack이호출된다() {
        var backPressed = false
        composeTestRule.setContent {
            WateringTheme {
                StatsScreen(onBack = { backPressed = true }, viewModel = createViewModel())
            }
        }

        composeTestRule.onNodeWithContentDescription("뒤로").performClick()

        assert(backPressed)
    }
}
