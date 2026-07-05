package com.watering.app.core.service

import android.content.Context
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Volume
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthConnectService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        // 수분 기록은 저장(write)만 함 — 로컬 DataStore가 이미 진실의 원천이라 읽기 권한은 불필요
        val HYDRATION_PERMISSIONS: Set<String> = setOf(
            HealthPermission.getWritePermission(HydrationRecord::class)
        )
    }

    val availability: HealthConnectAvailability
        get() = mapAvailability(HealthConnectClient.getSdkStatus(context))

    // Health Connect 미설치/미지원 기기에서는 client가 null — 호출부에서 반드시 null 체크
    val client: HealthConnectClient? by lazy {
        if (availability == HealthConnectAvailability.AVAILABLE) {
            HealthConnectClient.getOrCreate(context)
        } else {
            null
        }
    }

    suspend fun hasPermissions(permissions: Set<String>): Boolean {
        val granted = client?.permissionController?.getGrantedPermissions() ?: return false
        return granted.containsAll(permissions)
    }

    fun permissionRequestContract(): ActivityResultContract<Set<String>, Set<String>> =
        PermissionController.createRequestPermissionResultContract()

    // 물 마시기는 지속 시간이 없는 단일 이벤트지만, HydrationRecord는 startTime < endTime을
    // 요구해 종료 시각을 1ms 뒤로 둠(실측 검증 중 IllegalArgumentException으로 확인)
    suspend fun writeHydrationRecord(volumeMl: Double, timestampMillis: Long) {
        client?.insertRecords(
            listOf(
                HydrationRecord(
                    startTime = Instant.ofEpochMilli(timestampMillis),
                    startZoneOffset = null,
                    endTime = Instant.ofEpochMilli(timestampMillis + 1),
                    endZoneOffset = null,
                    volume = Volume.milliliters(volumeMl),
                    metadata = Metadata.manualEntry()
                )
            )
        )
    }
}

enum class HealthConnectAvailability {
    AVAILABLE,
    NOT_INSTALLED,
    UPDATE_REQUIRED
}

internal fun mapAvailability(sdkStatus: Int): HealthConnectAvailability = when (sdkStatus) {
    HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.AVAILABLE
    HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
        HealthConnectAvailability.UPDATE_REQUIRED
    else -> HealthConnectAvailability.NOT_INSTALLED
}
