package com.watering.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.watering.app.core.service.BillingService
import com.watering.app.core.service.NotificationService
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class WateringApp : Application(), Configuration.Provider {

    @Inject lateinit var notificationService: NotificationService
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var billingService: BillingService

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        notificationService.createChannels()
        notificationService.scheduleMidnightReset()
        billingService.startConnection()
    }
}
