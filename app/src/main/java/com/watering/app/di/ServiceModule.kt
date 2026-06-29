package com.watering.app.di

import android.content.Context
import com.watering.app.core.data.WaterRepository
import com.watering.app.core.service.NotificationService
import com.watering.app.core.service.WaterService
import com.watering.app.widget.WateringWidgetUpdater
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {

    @Provides
    @Singleton
    fun provideWateringWidgetUpdater(
        @ApplicationContext context: Context
    ): WateringWidgetUpdater = WateringWidgetUpdater(context)

    @Provides
    @Singleton
    fun provideWaterService(
        @ApplicationContext context: Context,
        repository: WaterRepository,
        updater: WateringWidgetUpdater
    ): WaterService = WaterService(context, repository, updater)

    @Provides
    @Singleton
    fun provideNotificationService(
        @ApplicationContext context: Context
    ): NotificationService = NotificationService(context)
}
