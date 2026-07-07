package com.watering.app.core.service

import android.content.Context
import android.util.Log
import com.watering.app.core.data.SettingsRepository
import com.watering.app.core.data.WaterRepository
import com.watering.app.core.model.DayRecord
import com.watering.app.core.model.DrinkType
import com.watering.app.core.model.StreakInfo
import com.watering.app.core.model.WaterUpdateResult
import com.watering.app.widget.WateringWidgetUpdater
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WaterService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: WaterRepository,
    private val widgetUpdater: WateringWidgetUpdater,
    private val settingsRepository: SettingsRepository,
    private val healthConnectService: HealthConnectService
) {
    companion object {
        private const val TAG = "WaterService"
    }

    suspend fun addWater(
        amount: Int,
        drinkType: DrinkType = DrinkType.WATER,
        goal: Int
    ): WaterUpdateResult {
        val result = repository.addEntry(amount, drinkType, goal)
        widgetUpdater.updateAll()
        syncToHealthConnectIfEnabled(result.updated)
        return result
    }

    // Health Connect 동기화는 부가 기능이라 실패해도 로컬 기록/위젯 갱신에는 영향 없어야 함.
    // 아래 두 조기 return은 로그 없이 조용히 스킵되던 것을 2026-07-07에 "동기화가 갑자기 안 된다"는
    // 제보를 진단하며 추가 — 설정 꺼짐/권한 미승인 둘 다 정상적인 skip 상태라 Log.w가 아닌 Log.d로 남김
    private suspend fun syncToHealthConnectIfEnabled(updated: DayRecord) {
        if (!settingsRepository.userSettings.first().healthConnectEnabled) {
            Log.d(TAG, "Health Connect 동기화 스킵: 설정에서 건강 연동이 꺼져있음")
            return
        }
        if (!healthConnectService.hasPermissions(HealthConnectService.HYDRATION_PERMISSIONS)) {
            Log.d(TAG, "Health Connect 동기화 스킵: WRITE_HYDRATION 권한 미승인")
            return
        }
        try {
            val entry = updated.entries.last()
            healthConnectService.writeHydrationRecord(
                volumeMl = entry.amount * entry.drinkType.hydrationRate,
                timestampMillis = entry.timestampMillis
            )
        } catch (e: Exception) {
            Log.w(TAG, "Health Connect 동기화 실패", e)
        }
    }

    suspend fun undoLastEntry(goal: Int): DayRecord {
        val updated = repository.removeLastEntry(goal)
        widgetUpdater.updateAll()
        return updated
    }

    suspend fun updateStreak(record: DayRecord, current: StreakInfo): StreakInfo =
        repository.updateStreak(record, current)

    suspend fun resetToday() {
        repository.resetToday()
        widgetUpdater.updateAll()
    }

    suspend fun clearAllData() {
        repository.clearAllData()
        widgetUpdater.updateAll()
    }
}
