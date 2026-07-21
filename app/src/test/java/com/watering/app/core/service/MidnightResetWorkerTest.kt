package com.watering.app.core.service

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MidnightResetWorkerTest {

    private lateinit var waterService: WaterService
    private lateinit var notificationService: NotificationService
    private lateinit var worker: MidnightResetWorker

    @Before
    fun setUp() {
        waterService = mockk()
        notificationService = mockk(relaxed = true)
        val context = mockk<Context>(relaxed = true)
        val params = mockk<WorkerParameters>(relaxed = true)
        worker = MidnightResetWorker(context, params, waterService, notificationService)
    }

    @Test
    fun doWork_정상적으로자정초기화하면success를반환하고다음자정작업을등록한다() = runTest {
        coEvery { waterService.resetTodayForMidnightRollover() } returns Unit

        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Success)
        coVerify { notificationService.scheduleMidnightReset() }
    }

    @Test
    fun doWork_초기화중예외가발생하면retry를반환하고다음자정작업은등록하지않는다() = runTest {
        coEvery { waterService.resetTodayForMidnightRollover() } throws RuntimeException("DataStore 오류")

        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Retry)
        coVerify(exactly = 0) { notificationService.scheduleMidnightReset() }
    }
}
