package com.watering.app

import android.app.Application
import android.content.ComponentCallbacks2
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.watering.app.core.service.BillingService
import com.watering.app.core.service.NotificationService
import com.watering.app.widget.WateringWidgetUpdater
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class WateringApp : Application(), Configuration.Provider {

    @Inject lateinit var notificationService: NotificationService
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var billingService: BillingService
    @Inject lateinit var widgetUpdater: WateringWidgetUpdater

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var lastNightMode = 0

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        notificationService.createChannels()
        notificationService.scheduleMidnightReset()
        billingService.startConnection()

        val currentNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        lastNightMode = currentNightMode

        // 다크모드 전환은 백그라운드 프로세스를 재시작시키는 경우가 많아(ComponentCallbacks만으로는
        // 못 잡는 경우가 있음) 마지막으로 그렸을 때의 모드를 SharedPreferences에 남겨 프로세스
        // 재시작 시점에도 감지되도록 이중으로 처리한다.
        val prefs = getSharedPreferences("app_state", MODE_PRIVATE)
        val persistedNightMode = prefs.getInt(KEY_LAST_NIGHT_MODE, currentNightMode)
        if (persistedNightMode != currentNightMode) {
            appScope.launch { widgetUpdater.updateAll() }
        }
        prefs.edit().putInt(KEY_LAST_NIGHT_MODE, currentNightMode).apply()

        // 프로세스가 살아있는 동안 다크모드가 전환되면(포그라운드 등) 위젯을 즉시 재렌더링
        registerComponentCallbacks(object : ComponentCallbacks2 {
            override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
                val newNightMode = newConfig.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                if (newNightMode != lastNightMode) {
                    lastNightMode = newNightMode
                    prefs.edit().putInt(KEY_LAST_NIGHT_MODE, newNightMode).apply()
                    appScope.launch { widgetUpdater.updateAll() }
                }
            }
            override fun onLowMemory() {}
            override fun onTrimMemory(level: Int) {}
        })
    }

    private companion object {
        const val KEY_LAST_NIGHT_MODE = "last_night_mode"
    }
}
