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
        val progress = if (goal > 0) current.toDouble() / goal else 0.0
        val (title, bodyTemplate) = pickReminderMessage(progress, LocalTime.now().hour)
        val body = String.format(java.util.Locale.getDefault(), bodyTemplate, current, goal)

        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(applicationContext, NotificationService.CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_water_drop)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(NotificationService.NOTIFICATION_ID_REMINDER, notification)
    }

    // 진행률(low/mid/near) 그룹 + 시간대(morning/afternoon/evening) 그룹 + 숫자 없는 감성형(generic)
    // 그룹을 합쳐서 무작위로 하나 선택. title/body는 같은 인덱스끼리 짝을 이룬다
    private fun pickReminderMessage(progress: Double, hour: Int): Pair<String, String> {
        val progressBucket = when {
            progress < 0.5 -> R.array.notification_reminder_titles_low to R.array.notification_reminder_bodies_low
            progress < 0.8 -> R.array.notification_reminder_titles_mid to R.array.notification_reminder_bodies_mid
            else -> R.array.notification_reminder_titles_near to R.array.notification_reminder_bodies_near
        }
        val timeBucket = when {
            hour < 12 -> R.array.notification_reminder_titles_morning to R.array.notification_reminder_bodies_morning
            hour < 18 -> R.array.notification_reminder_titles_afternoon to R.array.notification_reminder_bodies_afternoon
            else -> R.array.notification_reminder_titles_evening to R.array.notification_reminder_bodies_evening
        }
        val genericBucket = R.array.notification_reminder_titles_generic to R.array.notification_reminder_bodies_generic

        val titles = mutableListOf<String>()
        val bodies = mutableListOf<String>()
        for ((titleRes, bodyRes) in listOf(progressBucket, timeBucket, genericBucket)) {
            titles += applicationContext.resources.getStringArray(titleRes)
            bodies += applicationContext.resources.getStringArray(bodyRes)
        }
        val index = titles.indices.random()
        return titles[index] to bodies[index]
    }
}
