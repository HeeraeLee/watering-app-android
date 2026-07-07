package com.watering.app.di

import com.watering.app.core.data.SettingsRepository
import com.watering.app.core.data.WaterRepository
import com.watering.app.core.datastore.SettingsDataStore
import com.watering.app.core.datastore.WaterDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    // 날짜 의존 로직(오늘 기준일 계산 등)이 실제 시스템 시각을 직접 부르는 대신 주입받은 Clock을
    // 쓰도록 해서 테스트에서 고정 시각으로 교체 가능하게 한다 (CLAUDE.md 테스트 전략 참고)
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()

    @Provides
    @Singleton
    fun provideWaterRepository(dataStore: WaterDataStore): WaterRepository =
        WaterRepository(dataStore)

    @Provides
    @Singleton
    fun provideSettingsRepository(dataStore: SettingsDataStore): SettingsRepository =
        SettingsRepository(dataStore)
}
