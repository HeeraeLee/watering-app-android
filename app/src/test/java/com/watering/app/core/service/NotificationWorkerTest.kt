package com.watering.app.core.service

import android.content.Context
import androidx.work.WorkerParameters
import com.watering.app.core.data.SettingsRepository
import com.watering.app.core.data.WaterRepository
import com.watering.app.core.model.DayRecord
import com.watering.app.core.model.DrinkType
import com.watering.app.core.model.UserSettings
import com.watering.app.core.model.WaterEntry
import io.mockk.every
import io.mockk.mockk
import java.time.LocalTime
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// doWork()의 "알림을 실제로 표시하는" 경로(showReminderNotification — NotificationCompat.Builder
// 체이닝, PendingIntent, 리소스 배열 조회)는 순수 MockK로는 신뢰성 있게 검증하기 어려워 이 파일의
// 범위 밖으로 남긴다(Robolectric 필요). 여기선 알림을 보내기 "전에" 조기 반환하는 두 분기(시간대
// 밖/목표 이미 달성)만 검증 — 둘 다 showReminderNotification() 호출 전에 return하므로 안전하다.
// LocalTime.now()가 코드에 직접 박혀있어(Clock 미주입) 실제 "현재 시각"과 무관하게 결정적으로
// 테스트하기 위해, notificationStart/End를 실행 시점의 현재 시각을 기준으로 역산해서 구성한다.
class NotificationWorkerTest {

    private lateinit var waterRepository: WaterRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var worker: NotificationWorker

    @Before
    fun setUp() {
        waterRepository = mockk()
        settingsRepository = mockk()
        val context = mockk<Context>(relaxed = true)
        val params = mockk<WorkerParameters>(relaxed = true)
        worker = NotificationWorker(context, params, waterRepository, settingsRepository)
    }

    private fun record(entries: List<WaterEntry>, cupSize: Int = 200) =
        DayRecord(dateKey = "2026-07-21", entries = entries, cupSize = cupSize)

    @Test
    fun doWork_알림허용시간대밖이면알림을보내지않고success를반환한다() = runTest {
        val currentHour = LocalTime.now().hour
        // start == end == 현재 시각이면 "now >= end" 조건이 항상 참이 되어, 실행 시점의 실제
        // 시각과 무관하게 결정적으로 시간대 밖 분기를 탄다
        val settings = UserSettings(notificationStart = currentHour, notificationEnd = currentHour, dailyGoal = 8, cupSize = 200)
        every { settingsRepository.userSettings } returns flowOf(settings)
        every { waterRepository.todayRecord } returns flowOf(record(emptyList()))

        val result = worker.doWork()

        assertTrue(result is androidx.work.ListenableWorker.Result.Success)
    }

    @Test
    fun doWork_이미목표를달성했으면알림을보내지않고success를반환한다() = runTest {
        // start=0, end=24는 하루 24시간을 전부 포함해 실행 시점의 실제 시각과 무관하게
        // 항상 "시간대 안"으로 판정되므로, 목표 달성 분기만 독립적으로 검증할 수 있다
        val settings = UserSettings(notificationStart = 0, notificationEnd = 24, dailyGoal = 8, cupSize = 200)
        every { settingsRepository.userSettings } returns flowOf(settings)
        val entries = List(8) { WaterEntry(timestampMillis = it.toLong(), amount = 200, drinkType = DrinkType.WATER) }
        every { waterRepository.todayRecord } returns flowOf(record(entries))

        val result = worker.doWork()

        assertTrue(result is androidx.work.ListenableWorker.Result.Success)
    }
}
