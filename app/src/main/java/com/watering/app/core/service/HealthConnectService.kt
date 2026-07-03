package com.watering.app.core.service

import android.content.Context
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.WeightRecord
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

// Phase 3(체중 읽기 → 목표 추천), Phase 4(물 마시기 기록 자동 저장)가 공유하는 Health Connect 연동
// 기반 레이어. 이 서비스 자체는 어떤 화면에서도 아직 호출되지 않는다 (인프라만 구축하는 단계).
@Singleton
class HealthConnectService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        val HYDRATION_PERMISSIONS: Set<String> = setOf(
            HealthPermission.getReadPermission(HydrationRecord::class),
            HealthPermission.getWritePermission(HydrationRecord::class)
        )

        // Phase 3(스마트 목표 추천)에서 사용 예정 — 지금은 상수만 정의
        val WEIGHT_PERMISSIONS: Set<String> = setOf(
            HealthPermission.getReadPermission(WeightRecord::class)
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
