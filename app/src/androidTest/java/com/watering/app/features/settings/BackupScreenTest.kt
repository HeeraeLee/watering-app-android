package com.watering.app.features.settings

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.watering.app.core.service.AuthService
import com.watering.app.core.service.BackupService
import com.watering.app.ui.theme.WateringTheme
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

// 로그인 자체는 Credential Manager를 거치는 AuthService.signIn() 영역이라 범위 밖(AuthServiceTest
// 참고) — 로그아웃 상태 화면 표시와 뒤로가기만 검증한다.
class BackupScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun createViewModel(): BackupViewModel {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val authService = mockk<AuthService> {
            every { currentUser } returns MutableStateFlow(null)
        }
        val backupService = mockk<BackupService>(relaxed = true)
        return BackupViewModel(context, authService, backupService)
    }

    @Test
    fun 로그아웃상태에서_구글로그인안내와화면제목이보인다() {
        composeTestRule.setContent {
            WateringTheme {
                BackupScreen(onBack = {}, backupViewModel = createViewModel())
            }
        }

        composeTestRule.onNodeWithText("백업 및 복원").assertIsDisplayed()
        composeTestRule.onNodeWithText("구글 계정으로 로그인하면\n기록을 클라우드에 백업할 수 있어요.").assertIsDisplayed()
    }

    @Test
    fun 뒤로가기버튼을누르면_onBack이호출된다() {
        var backPressed = false
        composeTestRule.setContent {
            WateringTheme {
                BackupScreen(onBack = { backPressed = true }, backupViewModel = createViewModel())
            }
        }

        composeTestRule.onNodeWithContentDescription("뒤로").performClick()

        assert(backPressed)
    }
}
