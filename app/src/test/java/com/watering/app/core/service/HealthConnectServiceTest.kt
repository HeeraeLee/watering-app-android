package com.watering.app.core.service

import androidx.health.connect.client.HealthConnectClient
import org.junit.Assert.assertEquals
import org.junit.Test

class HealthConnectServiceTest {

    @Test
    fun mapAvailability_SDK사용가능이면AVAILABLE반환() {
        assertEquals(
            HealthConnectAvailability.AVAILABLE,
            mapAvailability(HealthConnectClient.SDK_AVAILABLE)
        )
    }

    @Test
    fun mapAvailability_업데이트필요면UPDATE_REQUIRED반환() {
        assertEquals(
            HealthConnectAvailability.UPDATE_REQUIRED,
            mapAvailability(HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED)
        )
    }

    @Test
    fun mapAvailability_미설치면NOT_INSTALLED반환() {
        assertEquals(
            HealthConnectAvailability.NOT_INSTALLED,
            mapAvailability(HealthConnectClient.SDK_UNAVAILABLE)
        )
    }

    @Test
    fun HYDRATION_PERMISSIONS_쓰기권한만포함한다() {
        assertEquals(1, HealthConnectService.HYDRATION_PERMISSIONS.size)
    }
}
