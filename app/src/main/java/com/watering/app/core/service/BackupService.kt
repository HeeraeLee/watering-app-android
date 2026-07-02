package com.watering.app.core.service

import com.google.firebase.firestore.FirebaseFirestore
import com.watering.app.core.data.SettingsRepository
import com.watering.app.core.data.WaterRepository
import com.watering.app.core.model.DayRecord
import com.watering.app.core.model.StreakInfo
import com.watering.app.core.model.UserSettings
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
    val settings: UserSettings,
    val backedUpAtMillis: Long
)

// 실시간 동기화가 아닌 수동 스냅샷 백업 — 사용자당 문서 1개(backups/{uid})만 유지하며,
// backup()은 항상 최신 스냅샷으로 덮어쓰고 restore()는 로컬 데이터를 전체 덮어쓴다.
@Singleton
class BackupService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val waterRepository: WaterRepository,
    private val settingsRepository: SettingsRepository
) {
    // encodeDefaults=true: 백업 시점의 실제 값을 그대로 보존한다.
    // 기본값 필드를 생략하면, 이후 앱 버전에서 기본값이 바뀔 때 복원 시 다른 값으로 잘못 채워질 수 있다.
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private object Fields {
        const val SCHEMA_VERSION = "schemaVersion"
        const val TODAY_RECORD = "todayRecord"
        const val STREAK_INFO = "streakInfo"
        const val HISTORY = "history"
        const val SETTINGS = "settings"
        const val BACKED_UP_AT = "backedUpAtMillis"
    }

    companion object {
        private const val COLLECTION = "backups"
        private const val SCHEMA_VERSION = 1
    }

    suspend fun backup(uid: String): Result<Long> = runCatching {
        val payload = BackupPayload(
            schemaVersion = SCHEMA_VERSION,
            todayRecord = waterRepository.todayRecord.first(),
            streakInfo = waterRepository.streakInfo.first(),
            history = waterRepository.getHistory().first(),
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

    private fun documentRef(uid: String) = firestore.collection(COLLECTION).document(uid)

    internal suspend fun applyPayload(payload: BackupPayload) {
        waterRepository.restoreAll(payload.todayRecord, payload.streakInfo, payload.history)
        settingsRepository.updateSettings(payload.settings)
    }

    internal fun toFirestoreMap(payload: BackupPayload): Map<String, Any> = mapOf(
        Fields.SCHEMA_VERSION to payload.schemaVersion,
        Fields.TODAY_RECORD to json.encodeToString(payload.todayRecord),
        Fields.STREAK_INFO to json.encodeToString(payload.streakInfo),
        Fields.HISTORY to json.encodeToString(payload.history),
        Fields.SETTINGS to json.encodeToString(payload.settings),
        Fields.BACKED_UP_AT to payload.backedUpAtMillis
    )

    internal fun fromFirestoreMap(map: Map<String, Any?>): BackupPayload = BackupPayload(
        schemaVersion = (map[Fields.SCHEMA_VERSION] as? Long)?.toInt() ?: SCHEMA_VERSION,
        todayRecord = json.decodeFromString(map[Fields.TODAY_RECORD] as? String ?: ""),
        streakInfo = json.decodeFromString(map[Fields.STREAK_INFO] as? String ?: ""),
        history = json.decodeFromString(map[Fields.HISTORY] as? String ?: ""),
        settings = json.decodeFromString(map[Fields.SETTINGS] as? String ?: ""),
        backedUpAtMillis = map[Fields.BACKED_UP_AT] as? Long ?: 0L
    )
}
