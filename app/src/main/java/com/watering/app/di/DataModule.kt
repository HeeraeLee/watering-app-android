package com.watering.app.di

import com.watering.app.core.data.SettingsRepository
import com.watering.app.core.data.WaterRepository
import com.watering.app.core.datastore.SettingsDataStore
import com.watering.app.core.datastore.WaterDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideWaterRepository(dataStore: WaterDataStore): WaterRepository =
        WaterRepository(dataStore)

    @Provides
    @Singleton
    fun provideSettingsRepository(dataStore: SettingsDataStore): SettingsRepository =
        SettingsRepository(dataStore)
}
