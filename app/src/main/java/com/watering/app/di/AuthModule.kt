package com.watering.app.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.watering.app.core.data.SettingsRepository
import com.watering.app.core.data.WaterRepository
import com.watering.app.core.service.AuthService
import com.watering.app.core.service.BackupService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideAuthService(
        @ApplicationContext context: Context,
        firebaseAuth: FirebaseAuth
    ): AuthService = AuthService(context, firebaseAuth)

    @Provides
    @Singleton
    fun provideBackupService(
        firestore: FirebaseFirestore,
        waterRepository: WaterRepository,
        settingsRepository: SettingsRepository
    ): BackupService = BackupService(firestore, waterRepository, settingsRepository)
}
