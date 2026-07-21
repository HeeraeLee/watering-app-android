package com.watering.app.core.datastore

import android.content.Context
import com.watering.app.core.model.UserSettings
import com.watering.app.core.model.WidgetTheme
import io.mockk.every
import io.mockk.mockk
import java.nio.file.Files
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// WaterDataStoreTest와 같은 이유(preferencesDataStore 델리게이트가 프로세스 전체에서 단일
// 인스턴스로 캐싱됨)로 파일 분리 대신 명시적 리셋으로 테스트를 격리한다. SettingsDataStore엔
// clearAllData()가 없어 updateSettings(UserSettings())로 기본값을 통째로 덮어써서 초기화한다.
class SettingsDataStoreTest {

    private lateinit var context: Context
    private lateinit var dataStore: SettingsDataStore

    @Before
    fun setUp() = runTest {
        val tempDir = Files.createTempDirectory("settingsDataStoreTest").toFile()
        context = mockk(relaxed = true)
        every { context.applicationContext } returns context
        every { context.filesDir } returns tempDir
        dataStore = SettingsDataStore(context)
        dataStore.updateSettings(UserSettings())
    }

    @Test
    fun updateSettings_저장한값이userSettings플로우에그대로반영된다() = runTest {
        val settings = UserSettings(
            dailyGoal = 10,
            cupSize = 300,
            weightKg = 65.5,
            widgetTheme = WidgetTheme.LAVENDER
        )

        dataStore.updateSettings(settings)

        assertEquals(settings, dataStore.userSettings.first())
    }

    @Test
    fun completeOnboarding_isOnboardingDone만true로바뀌고나머지필드는유지된다() = runTest {
        dataStore.updateSettings(UserSettings(dailyGoal = 12, cupSize = 250))

        dataStore.completeOnboarding()

        val result = dataStore.userSettings.first()
        assertTrue(result.isOnboardingDone)
        assertEquals(12, result.dailyGoal)
        assertEquals(250, result.cupSize)
    }

    @Test
    fun markReviewRequested_요청할때마다시각과누적횟수가함께갱신된다() = runTest {
        dataStore.markReviewRequested(1000L)
        dataStore.markReviewRequested(2000L)

        val result = dataStore.userSettings.first()
        assertEquals(2000L, result.lastReviewRequestedAtMillis)
        assertEquals(2, result.reviewRequestCount)
    }

    @Test
    fun userSettings_저장된값이없으면기본값을반환한다() = runTest {
        val result = dataStore.userSettings.first()

        assertEquals(UserSettings(), result)
    }
}
