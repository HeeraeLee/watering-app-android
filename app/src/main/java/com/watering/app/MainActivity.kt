package com.watering.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.watering.app.core.service.AnalyticsService
import com.watering.app.navigation.WateringNavGraph
import com.watering.app.ui.theme.WateringTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var analyticsService: AnalyticsService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val quickRecord = intent?.data?.scheme == "watering" &&
                intent?.data?.host == "quick-record"

        setContent {
            WateringTheme {
                WateringNavGraph(quickRecord = quickRecord, analyticsService = analyticsService)
            }
        }
    }
}
