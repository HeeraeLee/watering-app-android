package com.watering.app.core.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.watering.app.MainActivity
import com.watering.app.R
import com.watering.app.core.data.SettingsRepository
import com.watering.app.core.data.WaterRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalTime

@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val waterRepository: WaterRepository,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settings = settingsRepository.userSettings.first()
        val record = waterRepository.todayRecord.first()

        // 알림 시간대 외 또는 목표 달성 시 skip
        val now = LocalTime.now().hour
        if (now < settings.notificationStart || now >= settings.notificationEnd) return Result.success()
        if (record.isAchieved) return Result.success()

        // Android 13+ 권한 확인
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = applicationContext.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) return Result.success()
        }

        showReminderNotification(record.totalCount, settings.dailyGoal)
        return Result.success()
    }

    private fun showReminderNotification(current: Int, goal: Int) {
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(applicationContext, NotificationService.CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_water_drop)
            .setContentTitle(applicationContext.getString(R.string.notification_reminder_title))
            .setContentText(applicationContext.getString(R.string.notification_reminder_body, current, goal))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(NotificationService.NOTIFICATION_ID_REMINDER, notification)
    }
}
