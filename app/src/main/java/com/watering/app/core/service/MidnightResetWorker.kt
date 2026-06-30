package com.watering.app.core.service

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class MidnightResetWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val waterService: WaterService,
    private val notificationService: NotificationService
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            waterService.resetToday()
            notificationService.scheduleMidnightReset()
            Log.d("MidnightReset", "자정 초기화 완료, 다음 자정 작업 등록됨")
            Result.success()
        } catch (e: Exception) {
            Log.e("MidnightReset", "자정 초기화 실패", e)
            Result.retry()
        }
    }
}
