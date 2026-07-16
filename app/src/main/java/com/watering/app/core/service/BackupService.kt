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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

data class BackupPayload(
    val schemaVersion: Int,
    val todayRecord: DayRecord,
    val streakInfo: StreakInfo,
    val history: Map<String, DayRecord>,
    val annualHistory: Map<String, DailyAchievement>,
    val lifetimeAchievements: Set<String>,
    val settings: UserSettings,
    val backedUpAtMillis: Long
)

// 실시간 동기화가 아닌 수동 스냅샷 백업 — 사용자당 문서 1개(backups/{uid})만 유지하며,
// backup()은 항상 최신 스냅샷으로 덮어쓰고 restore()는 로컬 데이터를 전체 덮어쓴다.
@Singleton
class BackupService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val waterRepository: WaterRepository,
    private val settingsRepository: SettingsRepository,
    private val achievementDataStore: AchievementDataStore,
    private val widgetUpdater: WateringWidgetUpdater,
    private val notificationService: NotificationService
) {
    // encodeDefaults=true: 백업 시점의 실제 값을 그대로 보존한다.
    // 기본값 필드를 생략하면, 이후 앱 버전에서 기본값이 바뀔 때 복원 시 다른 값으로 잘못 채워질 수 있다.
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private object Fields {
        const val SCHEMA_VERSION = "schemaVersion"
        const val TODAY_RECORD = "todayRecord"
        const val STREAK_INFO = "streakInfo"
        const val HISTORY = "history"
        const val ANNUAL_HISTORY = "annualHistory"
        const val LIFETIME_ACHIEVEMENTS = "lifetimeAchievements"
        const val SETTINGS = "settings"
        const val BACKED_UP_AT = "backedUpAtMillis"
    }

    companion object {
        private const val COLLECTION = "backups"
        // v2(2026-07-16): annualHistory(연간 히트맵)/lifetimeAchievements(평생 업적) 추가
        private const val SCHEMA_VERSION = 2
    }

    suspend fun backup(uid: String): Result<Long> = runCatching {
        val payload = BackupPayload(
            schemaVersion = SCHEMA_VERSION,
            todayRecord = waterRepository.todayRecord.first(),
            streakInfo = waterRepository.streakInfo.first(),
            history = waterRepository.getHistory().first(),
            annualHistory = waterRepository.getAnnualHistory().first(),
            lifetimeAchievements = achievementDataStore.getLifetimeEarnedNames(),
            settings = settingsRepository.userSettings.first(),
            backedUpAtMillis = System.currentTimeMillis()
        )
        documentRef(uid).set(toFirestoreMap(payload)).await()
        payload.backedUpAtMillis
    }

    suspend fun restore(uid: String): Result<Unit> = runCatching {
        val snapshot = documentRef(uid).get().await()
        if (!snapshot.exists()) error("백업된 데이터가 없습니다")
        applyPayload(fromFirestoreMap(snapshot.data.orEmpty()))
    }

    suspend fun getLastBackupTimestamp(uid: String): Long? = runCatching {
        documentRef(uid).get().await().getLong(Fields.BACKED_UP_AT)
    }.getOrNull()

    suspend fun deleteBackup(uid: String): Result<Unit> = runCatching {
        documentRef(uid).delete().await()
    }

    private fun documentRef(uid: String) = firestore.collection(COLLECTION).document(uid)

    // 복원 직후 위젯/알림을 갱신하지 않으면 다음 앱 재시작 전까지 화면·리마인더가 복원 전 데이터로
    // 남아있었음(2026-07-16 수정) — SettingsViewModel.update()와 동일한 갱신 패턴
    internal suspend fun applyPayload(payload: BackupPayload) {
        waterRepository.restoreAll(payload.todayRecord, payload.streakInfo, payload.history, payload.annualHistory)
        achievementDataStore.restoreLifetimeEarned(payload.lifetimeAchievements)
        settingsRepository.updateSettings(payload.settings)
        widgetUpdater.updateAll()
        notificationService.scheduleReminders(payload.settings)
    }

    internal fun toFirestoreMap(payload: BackupPayload): Map<String, Any> = mapOf(
        Fields.SCHEMA_VERSION to payload.schemaVersion,
        Fields.TODAY_RECORD to json.encodeToString(payload.todayRecord),
        Fields.STREAK_INFO to json.encodeToString(payload.streakInfo),
        Fields.HISTORY to json.encodeToString(payload.history),
        Fields.ANNUAL_HISTORY to json.encodeToString(payload.annualHistory),
        Fields.LIFETIME_ACHIEVEMENTS to json.encodeToString(payload.lifetimeAchievements),
        Fields.SETTINGS to json.encodeToString(payload.settings),
        Fields.BACKED_UP_AT to payload.backedUpAtMillis
    )

    // annualHistory/lifetimeAchievements는 v1(스키마 버전 1) 백업엔 없는 필드라, 옛 백업을
    // 복원할 때 필드가 아예 없어도 실패하지 않고 빈 값으로 채운다
    internal fun fromFirestoreMap(map: Map<String, Any?>): BackupPayload = BackupPayload(
        schemaVersion = (map[Fields.SCHEMA_VERSION] as? Long)?.toInt() ?: SCHEMA_VERSION,
        todayRecord = json.decodeFromString(map[Fields.TODAY_RECORD] as? String ?: ""),
        streakInfo = json.decodeFromString(map[Fields.STREAK_INFO] as? String ?: ""),
        history = json.decodeFromString(map[Fields.HISTORY] as? String ?: ""),
        annualHistory = (map[Fields.ANNUAL_HISTORY] as? String)?.takeIf { it.isNotBlank() }
            ?.let { json.decodeFromString<Map<String, DailyAchievement>>(it) } ?: emptyMap(),
        lifetimeAchievements = (map[Fields.LIFETIME_ACHIEVEMENTS] as? String)?.takeIf { it.isNotBlank() }
            ?.let { json.decodeFromString<Set<String>>(it) } ?: emptySet(),
        settings = json.decodeFromString(map[Fields.SETTINGS] as? String ?: ""),
        backedUpAtMillis = map[Fields.BACKED_UP_AT] as? Long ?: 0L
    )
}
