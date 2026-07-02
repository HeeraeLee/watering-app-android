package com.watering.app.core.service

import com.google.firebase.firestore.FirebaseFirestore
import com.watering.app.core.data.SettingsRepository
import com.watering.app.core.data.WaterRepository
import com.watering.app.core.model.DayRecord
import com.watering.app.core.model.StreakInfo
import com.watering.app.core.model.UserSettings
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
    private lateinit var service: BackupService

    private val payload = BackupPayload(
        schemaVersion = 1,
        todayRecord = DayRecord(dateKey = "2026-07-02", goal = 8),
        streakInfo = StreakInfo(currentStreak = 3, longestStreak = 5),
        history = mapOf("2026-07-01" to DayRecord(dateKey = "2026-07-01", goal = 8)),
        settings = UserSettings(dailyGoal = 8, cupSize = 200),
        backedUpAtMillis = 1_000_000L
    )

    @Before
    fun setUp() {
        firestore = mockk(relaxed = true)
        waterRepository = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)
        service = BackupService(firestore, waterRepository, settingsRepository)
    }

    @Test
    fun toFirestoreMap_모든필드를JSON문자열과원시타입으로변환한다() {
        val map = service.toFirestoreMap(payload)

        assertEquals(1, map["schemaVersion"])
        assertEquals(1_000_000L, map["backedUpAtMillis"])
        assertEquals(true, (map["todayRecord"] as String).contains("2026-07-02"))
        assertEquals(true, (map["settings"] as String).contains("\"dailyGoal\":8"))
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
        map["schemaVersion"] = 1L // Firestore는 정수를 Long으로 반환

        val restored = service.fromFirestoreMap(map)

        assertEquals(1, restored.schemaVersion)
    }

    @Test
    fun applyPayload_water와settings레포지토리에복원을위임한다() = runTest {
        coEvery { waterRepository.restoreAll(any(), any(), any()) } returns Unit
        coEvery { settingsRepository.updateSettings(any()) } returns Unit

        service.applyPayload(payload)

        coVerify {
            waterRepository.restoreAll(payload.todayRecord, payload.streakInfo, payload.history)
            settingsRepository.updateSettings(payload.settings)
        }
    }
}
