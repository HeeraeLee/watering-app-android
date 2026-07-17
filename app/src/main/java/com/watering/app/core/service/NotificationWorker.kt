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
import kotlin.math.floor

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
        // record.cupSize는 마지막 기록 시점의 스냅샷이라 컵 크기를 바꾼 직후 다음 기록 전까지
        // stale할 수 있음 — goal과 동일하게 항상 최신 settings.cupSize로 직접 재계산한다
        val cupSize = settings.cupSize.coerceAtLeast(1)
        val totalCountExact = record.entries.sumOf { it.amount }.toDouble() / cupSize

        // 알림 시간대 외 또는 목표 달성 시 skip
        val now = LocalTime.now().hour
        if (now < settings.notificationStart || now >= settings.notificationEnd) return Result.success()
        // record.isAchieved(마지막 기록 시점의 옛 goal)로 skip을 판정하면 아래 본문에 쓰는
        // settings.dailyGoal(현재)과 서로 다른 목표를 기준으로 삼게 돼, 목표를 바꾼 직후
        // skip 여부와 본문 진행률이 서로 모순될 수 있었음 — 항상 최신 settings.dailyGoal 하나로 통일
        if (totalCountExact >= settings.dailyGoal) return Result.success()

        // Android 13+ 권한 확인
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = applicationContext.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) return Result.success()
        }

        showReminderNotification(floor(totalCountExact).toInt(), settings.dailyGoal)
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
