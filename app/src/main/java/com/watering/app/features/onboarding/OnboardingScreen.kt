package com.watering.app.features.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onComplete: () -> Unit
) {
    var page by remember { mutableIntStateOf(0) }
    var dailyGoal by remember { mutableIntStateOf(8) }
    var cupSize by remember { mutableIntStateOf(200) }
    var notificationEnabled by remember { mutableStateOf(true) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> notificationEnabled = granted }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (page) {
            0 -> OnboardingPage1()
            1 -> OnboardingPage2(
                goal = dailyGoal,
                cupSize = cupSize,
                onGoalChange = { dailyGoal = it },
                onCupSizeChange = { cupSize = it }
            )
            2 -> OnboardingPage3()
        }

        Spacer(Modifier.height(48.dp))

        Button(
            onClick = {
                if (page < 2) {
                    if (page == 1 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    page++
                } else {
                    viewModel.completeOnboarding(dailyGoal, cupSize, notificationEnabled)
                    onComplete()
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(if (page < 2) "다음" else "시작하기", fontSize = 18.sp)
        }
    }
}

@Composable
private fun OnboardingPage1() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("💧", fontSize = 80.sp)
        Spacer(Modifier.height(24.dp))
        Text("워터링", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(
            "잠금화면에서 바로 기록하는\n가장 쉬운 물 마시기 앱",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun OnboardingPage2(
    goal: Int,
    cupSize: Int,
    onGoalChange: (Int) -> Unit,
    onCupSizeChange: (Int) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("목표를 설정해요", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(32.dp))

        Text("하루 목표: $goal 잔", style = MaterialTheme.typography.bodyLarge)
        androidx.compose.material3.Slider(
            value = goal.toFloat(),
            onValueChange = { onGoalChange(it.toInt()) },
            valueRange = 1f..20f,
            steps = 18
        )

        Spacer(Modifier.height(24.dp))

        Text("컵 크기: ${cupSize}ml", style = MaterialTheme.typography.bodyLarge)
        val cupSizes = listOf(150, 200, 250, 300, 350, 500)
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            cupSizes.forEach { size ->
                androidx.compose.material3.FilterChip(
                    selected = cupSize == size,
                    onClick = { onCupSizeChange(size) },
                    label = { Text("${size}ml") }
                )
            }
        }
    }
}

@Composable
private fun OnboardingPage3() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🔔", fontSize = 64.sp)
        Spacer(Modifier.height(24.dp))
        Text("알림을 허용하면\n잊지 않고 물을 마실 수 있어요", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(12.dp))
        Text(
            "다음 단계에서 알림 권한을 요청합니다",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
