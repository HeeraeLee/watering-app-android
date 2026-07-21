package com.watering.app.features.home

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.watering.app.core.data.SettingsRepository
import com.watering.app.core.data.WaterRepository
import com.watering.app.core.datastore.AchievementDataStore
import com.watering.app.core.model.DayRecord
import com.watering.app.core.model.StreakInfo
import com.watering.app.core.model.UserSettings
import com.watering.app.core.service.AchievementChecker
import com.watering.app.core.service.AnalyticsService
import com.watering.app.core.service.ReviewService
import com.watering.app.core.service.WaterService
import com.watering.app.ui.theme.WateringTheme
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

// HomeScreen()이 hiltViewModel()을 직접 호출하지 않고 viewModel을 파라미터로 받는 구조라,
// HomeViewModelTest.kt와 동일하게(가짜 의존성을 주입한) 실제 HomeViewModel을 직접 생성해 넘긴다 —
// Hilt 테스트 러너/DI 오버라이드가 전혀 필요 없다. 실기기(InstrumentationRegistry) 위에서 도는
// 첫 Compose UI 테스트라 이 프로젝트 androidTest 소스셋 자체가 이번에 처음 생긴다.
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun createViewModel(
        record: DayRecord = DayRecord(dateKey = "2026-07-21", goal = 8),
        streak: StreakInfo = StreakInfo(currentStreak = 3, longestStreak = 10)
    ): HomeViewModel {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val waterService = mockk<WaterService>(relaxed = true)
        val waterRepository = mockk<WaterRepository> {
            every { todayRecord } returns MutableStateFlow(record)
            every { streakInfo } returns MutableStateFlow(streak)
        }
        val settingsRepository = mockk<SettingsRepository> {
            every { userSettings } returns MutableStateFlow(UserSettings(dailyGoal = 8, cupSize = 200))
        }
        val achievementChecker = mockk<AchievementChecker>()
        val achievementDataStore = mockk<AchievementDataStore> {
            coEvery { consumePendingDisplay() } returns null
        }
        val reviewService = mockk<ReviewService>(relaxed = true)
        val analyticsService = mockk<AnalyticsService>(relaxed = true)
        return HomeViewModel(
            context, waterService, waterRepository, settingsRepository,
            achievementChecker, achievementDataStore, reviewService, analyticsService
        )
    }

    @Test
    fun 기록이없는초기상태에서_빈기록안내와물마셨어요버튼이보인다() {
        val viewModel = createViewModel(record = DayRecord(dateKey = "2026-07-21", goal = 8, entries = emptyList()))

        composeTestRule.setContent {
            WateringTheme {
                HomeScreen(viewModel = viewModel, onNavigateToStats = {}, onNavigateToSettings = {})
            }
        }

        composeTestRule.onNodeWithText("물 마셨어요").assertIsDisplayed()
        composeTestRule.onNodeWithText("아직 기록이 없어요.\n물을 마시고 기록해 보세요!").assertIsDisplayed()
    }

    @Test
    fun 연속기록카드에currentStreak과longestStreak값이표시된다() {
        val viewModel = createViewModel(streak = StreakInfo(currentStreak = 5, longestStreak = 12))

        composeTestRule.setContent {
            WateringTheme {
                HomeScreen(viewModel = viewModel, onNavigateToStats = {}, onNavigateToSettings = {})
            }
        }

        composeTestRule.onNodeWithText("5일").assertIsDisplayed()
        composeTestRule.onNodeWithText("12일").assertIsDisplayed()
    }
}
