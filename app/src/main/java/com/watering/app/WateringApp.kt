package com.watering.app

import android.app.Application
import com.watering.app.core.service.NotificationService
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class WateringApp : Application() {

    @Inject
    lateinit var notificationService: NotificationService

    override fun onCreate() {
        super.onCreate()
        notificationService.createChannels()
    }
}
