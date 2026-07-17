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
        goal: Int,
        cupSize: Int
    ): WaterUpdateResult {
        val result = repository.addEntry(amount, drinkType, goal, cupSize)
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

    suspend fun undoLastEntry(goal: Int, cupSize: Int): DayRecord {
        val updated = repository.removeLastEntry(goal, cupSize)
        widgetUpdater.updateAll()
        return updated
    }

    suspend fun updateStreak(record: DayRecord, current: StreakInfo): StreakInfo =
        repository.updateStreak(record, current)

    suspend fun rollbackStreakAfterUndo(record: DayRecord, current: StreakInfo): StreakInfo =
        repository.rollbackStreakAfterUndo(record, current)

    // 사용자가 명시적으로 요청한 초기화(컵 크기 변경 다이얼로그의 "초기화") 전용 — 항상 무조건
    // 오늘 기록을 지운다. 자정 롤오버에는 이 함수를 쓰지 않는다(resetTodayForMidnightRollover
    // 참고) — 자정 워커가 지연 실행되면 그 사이 이미 기록된 오늘 데이터까지 지워버리는 문제가
    // 있었음(2026-07-16). 오늘 이미 목표를 달성해 streak이 올라간 상태였다면, 초기화로 오늘이
    // 다시 미달성이 되므로 undo와 동일하게 되돌린다.
    suspend fun resetToday() {
        val settings = settingsRepository.userSettings.first()
        val currentStreak = repository.streakInfo.first()
        val updated = repository.resetToday(settings.dailyGoal, settings.cupSize)
        repository.rollbackStreakAfterUndo(updated, currentStreak)
        widgetUpdater.updateAll()
    }

    // MidnightResetWorker 전용 — 기기가 꺼져있다 늦게 켜지면 이 워커가 실제 자정보다 한참 뒤에
    // 실행될 수 있다. 그 사이 위젯/앱에서 이미 오늘 날짜로 기록이 시작됐다면(addEntry의 자정
    // 자동 롤오버로 자연히 새 하루가 시작된 경우) 그 기록을 지우면 안 되므로, resetToday()와
    // 달리 TODAY_RECORD가 아직 어제 날짜에 머물러 있을 때만(=진짜 stale할 때만) 초기화한다
    // (2026-07-16, 자정 리셋 지연 시 당일 기록 소실 버그 수정).
    suspend fun resetTodayForMidnightRollover() {
        val settings = settingsRepository.userSettings.first()
        val updated = repository.resetTodayIfStale(settings.dailyGoal, settings.cupSize) ?: return
        val currentStreak = repository.streakInfo.first()
        repository.rollbackStreakAfterUndo(updated, currentStreak)
        widgetUpdater.updateAll()
    }

    // SettingsViewModel이 컵 크기 변경 확인/재계산을 위해 "오늘 이미 마신 기록"을 스냅샷으로
    // 한 번 읽어야 해서 추가(2026-07-16). 지속 구독이 아니므로 Flow 대신 suspend 함수로 노출.
    suspend fun currentTodayRecord(): DayRecord = repository.todayRecord.first()

    // Home 화면은 오늘 record.goal을 항상 최신 settings.dailyGoal로 덮어써서 보여주므로(item①과
    // 동일한 패턴), 목표를 낮추면 화면엔 그 자리에서 "달성!"이 뜨지만 streak은 addWater/undo
    // 경로에서만 갱신돼 그대로 남아있는 모순이 있었음 — 설정에서 목표가 바뀔 때마다 호출해 동기화한다.
    //
    // "현재 streak에 오늘이 이미 반영돼 있는지"는 저장된 today.goal이 아니라
    // streak.lastAchievedDateKey로 판단한다 — today.goal(DayRecord 스냅샷)은 기록을 남길 때만
    // 갱신되므로, 기록 없이 설정만 여러 번 바꾸는 상황(목표를 낮췄다 다시 올리는 등)에서는 이전
    // 상태를 반영하지 못해 롤백이 누락됨. 달성 방향으로 넘어가면 updateStreak(),
    // 미달성으로 되돌아가면 undo와 동일한 rollbackStreakAfterUndo()를 호출해 대칭적으로 처리한다
    // (2026-07-17, 목표를 낮췄다 올리는 것만으로 streak이 영구 증가하던 악용 경로 수정 —
    // updateDailyGoal/applyCupSizeChange/applyWeightGoal 세 경로 모두 이 함수를 거치므로 함께 막힘).
    suspend fun syncStreakForGoalChange(newGoal: Int) {
        val today = repository.todayRecord.first()
        val current = repository.streakInfo.first()
        val updatedRecord = today.copy(goal = newGoal)
        val creditedToday = current.lastAchievedDateKey == today.dateKey
        when {
            !creditedToday && updatedRecord.isAchieved -> repository.updateStreak(updatedRecord, current)
            creditedToday && !updatedRecord.isAchieved -> repository.rollbackStreakAfterUndo(updatedRecord, current)
        }
    }

    suspend fun clearAllData() {
        repository.clearAllData()
        widgetUpdater.updateAll()
    }
}
