package com.watering.app.core.service

import com.google.firebase.firestore.FirebaseFirestore
import com.watering.app.core.data.SettingsRepository
import com.watering.app.core.data.WaterRepository
import com.watering.app.core.datastore.AchievementDataStore
import com.watering.app.core.model.DailyAchievement
import com.watering.app.core.model.DayRecord
import com.watering.app.core.model.StreakInfo
import com.watering.app.core.model.UserSettings
import com.watering.app.widget.WateringWidgetUpdater
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class BackupServiceTest {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var waterRepository: WaterRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var achievementDataStore: AchievementDataStore
    private lateinit var widgetUpdater: WateringWidgetUpdater
    private lateinit var notificationService: NotificationService
    private lateinit var service: BackupService

    private val payload = BackupPayload(
        schemaVersion = 2,
        todayRecord = DayRecord(dateKey = "2026-07-02", goal = 8),
        streakInfo = StreakInfo(currentStreak = 3, longestStreak = 5),
        history = mapOf("2026-07-01" to DayRecord(dateKey = "2026-07-01", goal = 8)),
        annualHistory = mapOf(
            "2026-07-01" to DailyAchievement(dateKey = "2026-07-01", totalCount = 8.0, goal = 8)
        ),
        lifetimeAchievements = setOf("STREAK_7", "STREAK_30"),
        settings = UserSettings(dailyGoal = 8, cupSize = 200),
        backedUpAtMillis = 1_000_000L
    )

    @Before
    fun setUp() {
        firestore = mockk(relaxed = true)
        waterRepository = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)
        achievementDataStore = mockk(relaxed = true)
        widgetUpdater = mockk(relaxed = true)
        notificationService = mockk(relaxed = true)
        service = BackupService(
            firestore,
            waterRepository,
            settingsRepository,
            achievementDataStore,
            widgetUpdater,
            notificationService
        )
    }

    @Test
    fun toFirestoreMap_모든필드를JSON문자열과원시타입으로변환한다() {
        val map = service.toFirestoreMap(payload)

        assertEquals(2, map["schemaVersion"])
        assertEquals(1_000_000L, map["backedUpAtMillis"])
        assertEquals(true, (map["todayRecord"] as String).contains("2026-07-02"))
        assertEquals(true, (map["settings"] as String).contains("\"dailyGoal\":8"))
        assertEquals(true, (map["annualHistory"] as String).contains("2026-07-01"))
        assertEquals(true, (map["lifetimeAchievements"] as String).contains("STREAK_7"))
    }

    @Test
    fun fromFirestoreMap_toFirestoreMap과왕복하면원본payload와동일하다() {
        val map = service.toFirestoreMap(payload)

        val restored = service.fromFirestoreMap(map)

        assertEquals(payload, restored)
    }

    @Test
    fun fromFirestoreMap_schemaVersion이Long으로들어와도정상변환된다() {
        val map = service.toFirestoreMap(payload).toMutableMap()
        map["schemaVersion"] = 2L // Firestore는 정수를 Long으로 반환

        val restored = service.fromFirestoreMap(map)

        assertEquals(2, restored.schemaVersion)
    }

    @Test
    fun fromFirestoreMap_v1백업처럼annualHistory와lifetimeAchievements필드가없어도빈값으로채운다() {
        // v1 스키마 백업을 흉내내기 위해 새로 추가된 두 필드만 제거 — 나머지는 실제 인코더가 만든
        // 유효한 JSON을 그대로 써서 model 필드 구성이 바뀌어도 이 테스트가 깨지지 않게 한다
        val v1Map = service.toFirestoreMap(payload).toMutableMap()
        v1Map.remove("annualHistory")
        v1Map.remove("lifetimeAchievements")
        v1Map["schemaVersion"] = 1L

        val restored = service.fromFirestoreMap(v1Map)

        assertEquals(1, restored.schemaVersion)
        assertEquals(emptyMap<String, DailyAchievement>(), restored.annualHistory)
        assertEquals(emptySet<String>(), restored.lifetimeAchievements)
    }

    @Test
    fun applyPayload_모든repository에복원을위임하고위젯과알림을갱신한다() = runTest {
        coEvery { waterRepository.restoreAll(any(), any(), any(), any()) } returns Unit
        coEvery { settingsRepository.updateSettings(any()) } returns Unit

        service.applyPayload(payload)

        coVerify {
            waterRepository.restoreAll(payload.todayRecord, payload.streakInfo, payload.history, payload.annualHistory)
            achievementDataStore.restoreLifetimeEarned(payload.lifetimeAchievements)
            settingsRepository.updateSettings(payload.settings)
            widgetUpdater.updateAll()
            notificationService.scheduleReminders(payload.settings)
        }
    }
}
