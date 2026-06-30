package com.watering.app.core.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.watering.app.MainActivity
import com.watering.app.R
import com.watering.app.core.model.UserSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_REMINDER = "watering_reminder"
        const val CHANNEL_ACHIEVEMENT = "watering_achievement"
        const val WORK_REMINDER = "watering_reminder_work"
        const val WORK_MIDNIGHT_RESET = "watering_midnight_reset"
        const val NOTIFICATION_ID_REMINDER = 1001
        const val NOTIFICATION_ID_ACHIEVEMENT = 1002
    }

    fun createChannels() {
        val reminderChannel = NotificationChannel(
            CHANNEL_REMINDER,
            context.getString(R.string.notification_channel_reminder),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_reminder_desc)
        }
        val achievementChannel = NotificationChannel(
            CHANNEL_ACHIEVEMENT,
            context.getString(R.string.notification_channel_achievement),
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(listOf(reminderChannel, achievementChannel))
    }

    fun scheduleReminders(settings: UserSettings) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(WORK_REMINDER)

        if (!settings.notificationEnabled) return

        val request = PeriodicWorkRequestBuilder<NotificationWorker>(
            settings.notificationInterval.toLong(), TimeUnit.MINUTES,
            (settings.notificationInterval / 2).toLong(), TimeUnit.MINUTES
        ).build()

        workManager.enqueueUniquePeriodicWork(
            WORK_REMINDER,
            ExistingPeriodicWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelReminders() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_REMINDER)
    }

    fun scheduleMidnightReset() {
        val now = LocalDateTime.now()
        val midnight = now.toLocalDate().plusDays(1).atStartOfDay()
        val delaySeconds = ChronoUnit.SECONDS.between(now, midnight)

        val request = OneTimeWorkRequestBuilder<MidnightResetWorker>()
            .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_MIDNIGHT_RESET,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun showAchievementNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return  // 권한 없으면 skip

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ACHIEVEMENT)
            .setSmallIcon(R.drawable.ic_water_drop)
            .setContentTitle(context.getString(R.string.notification_achievement_title))
            .setContentText(context.getString(R.string.notification_achievement_body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_ACHIEVEMENT, notification)
    }
}
