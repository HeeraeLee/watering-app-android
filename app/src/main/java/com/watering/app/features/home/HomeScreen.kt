package com.watering.app.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.watering.app.core.model.DrinkType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    quickRecord: Boolean = false,
    onNavigateToStats: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPremium: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // 위젯 탭으로 진입한 경우 즉시 기록
    LaunchedEffect(quickRecord) {
        if (quickRecord) viewModel.addWater()
    }

    // 스낵바 표시 (Undo 포함)
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            val result = snackbarHostState.showSnackbar(
                message = msg,
                actionLabel = "취소"
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoLastEntry()
            }
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("워터링 💧", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToStats) {
                        Icon(Icons.Default.BarChart, contentDescription = "통계")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "설정")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            // 진행률 링
            AchievementRing(
                current = uiState.record.totalCount,
                goal = uiState.record.goal,
                rate = uiState.record.achievementRate
            )

            Spacer(Modifier.height(32.dp))

            // 1탭 기록 버튼
            Button(
                onClick = { viewModel.addWater() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("물 마셨어요 💧", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(12.dp))

            // 스트릭 표시
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StreakCard(label = "현재 스트릭", value = "${uiState.streak.currentStreak}일")
                StreakCard(label = "최장 스트릭", value = "${uiState.streak.longestStreak}일")
            }

            Spacer(Modifier.height(16.dp))

            // 오늘 기록 타임라인
            Text(
                "오늘 기록",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            if (uiState.record.entries.isEmpty()) {
                Text(
                    "아직 기록이 없어요. 물을 마시고 기록해 보세요!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn {
                    items(uiState.record.entries.reversed()) { entry ->
                        WaterEntryRow(entry = entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementRing(current: Int, goal: Int, rate: Double) {
    val ringColor = when {
        rate >= 1.0 -> Color(0xFF4CAF50)
        rate >= 0.7 -> Color(0xFF007AFF)
        rate >= 0.3 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp)) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 16.dp.toPx()
            val sweepAngle = (rate.coerceIn(0.0, 1.0) * 360f).toFloat()
            drawArc(
                color = ringColor.copy(alpha = 0.2f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth)
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    strokeWidth,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$current",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = ringColor
            )
            Text(text = "/ $goal 잔", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "${(rate * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StreakCard(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun WaterEntryRow(entry: com.watering.app.core.model.WaterEntry) {
    val emoji = when (entry.drinkType) {
        DrinkType.WATER -> "💧"
        DrinkType.COFFEE -> "☕"
        DrinkType.TEA -> "🍵"
        DrinkType.JUICE -> "🧃"
        DrinkType.MILK -> "🥛"
        DrinkType.OTHER -> "🫗"
    }
    val time = java.time.Instant.ofEpochMilli(entry.timestampMillis)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalTime()
        .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$emoji ${entry.amount}ml", style = MaterialTheme.typography.bodyMedium)
        Text(time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
