package com.watering.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.watering.app.navigation.WateringNavGraph
import com.watering.app.ui.theme.WateringTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val quickRecord = intent?.data?.scheme == "watering" &&
                intent?.data?.host == "quick-record"

        setContent {
            WateringTheme {
                WateringNavGraph(quickRecord = quickRecord)
            }
        }
    }
}
