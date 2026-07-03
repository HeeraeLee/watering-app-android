package com.watering.app

import android.content.Intent
import android.net.Uri
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

        // Health Connect가 권한 요청 화면에 표시할 개인정보처리방침을 요구할 때 시스템이 보내는 인텐트
        if (intent?.action == "androidx.health.connect.action.SHOW_PERMISSIONS_RATIONALE") {
            startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.privacy_policy_url)))
            )
            finish()
            return
        }

        val quickRecord = intent?.data?.scheme == "watering" &&
                intent?.data?.host == "quick-record"

        setContent {
            WateringTheme {
                WateringNavGraph(quickRecord = quickRecord, analyticsService = analyticsService)
            }
        }
    }
}
