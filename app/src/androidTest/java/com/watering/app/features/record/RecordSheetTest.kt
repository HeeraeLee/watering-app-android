package com.watering.app.features.record

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.watering.app.core.data.SettingsRepository
import com.watering.app.core.model.UserSettings
import com.watering.app.ui.theme.WateringTheme
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

// RecordSheet()도 hiltViewModel()이 기본값일 뿐이라, HomeScreenTest와 동일하게 가짜 의존성을
// 주입한 진짜 RecordViewModel을 직접 만들어 파라미터로 넘긴다.
class RecordSheetTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun createViewModel(cupSize: Int = 200): RecordViewModel {
        val settingsRepository = mockk<SettingsRepository> {
            every { userSettings } returns MutableStateFlow(UserSettings(cupSize = cupSize))
        }
        return RecordViewModel(settingsRepository)
    }

    @Test
    fun 초기상태는_물이선택되고컵크기만큼기록버튼에표시된다() {
        val viewModel = createViewModel(cupSize = 200)

        composeTestRule.setContent {
            WateringTheme {
                RecordSheet(onDismiss = {}, onRecord = { _, _ -> }, viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText("💧 물 200ml 기록하기").assertIsDisplayed()
    }

    @Test
    fun 음료종류를바꾸면_해당음료의첫번째프리셋양으로기록버튼이갱신된다() {
        val viewModel = createViewModel(cupSize = 200)

        composeTestRule.setContent {
            WateringTheme {
                RecordSheet(onDismiss = {}, onRecord = { _, _ -> }, viewModel = viewModel)
            }
        }
        composeTestRule.onNodeWithText("커피").performClick()

        // presetAmountsFor(COFFEE)의 첫 값은 355ml(스타벅스 톨)
        composeTestRule.onNodeWithText("☕ 커피 355ml 기록하기").assertIsDisplayed()
    }

    @Test
    fun 다른양프리셋칩을누르면_기록버튼의양이갱신된다() {
        val viewModel = createViewModel(cupSize = 200)

        composeTestRule.setContent {
            WateringTheme {
                RecordSheet(onDismiss = {}, onRecord = { _, _ -> }, viewModel = viewModel)
            }
        }
        composeTestRule.onNodeWithText("887ml").performClick()

        composeTestRule.onNodeWithText("💧 물 887ml 기록하기").assertIsDisplayed()
        composeTestRule.onNodeWithText("887ml").assertIsSelected()
    }

    @Test
    fun 기록하기버튼을누르면_선택된양과음료타입으로onRecord가호출되고onDismiss도호출된다() {
        val viewModel = createViewModel(cupSize = 200)
        var recordedAmount: Int? = null
        var recordedType: com.watering.app.core.model.DrinkType? = null
        var dismissed = false

        composeTestRule.setContent {
            WateringTheme {
                RecordSheet(
                    onDismiss = { dismissed = true },
                    onRecord = { amount, type -> recordedAmount = amount; recordedType = type },
                    viewModel = viewModel
                )
            }
        }
        composeTestRule.onNodeWithText("💧 물 200ml 기록하기").performClick()

        assertEquals(200, recordedAmount)
        assertEquals(com.watering.app.core.model.DrinkType.WATER, recordedType)
        assertEquals(true, dismissed)
    }

    @Test
    fun 직접입력칩을누르면_입력필드가나타나고초기값은컵크기다() {
        val viewModel = createViewModel(cupSize = 200)

        composeTestRule.setContent {
            WateringTheme {
                RecordSheet(onDismiss = {}, onRecord = { _, _ -> }, viewModel = viewModel)
            }
        }
        composeTestRule.onNodeWithText("직접 입력").performClick()

        composeTestRule.onNodeWithText("200").assertIsDisplayed()
    }
}
