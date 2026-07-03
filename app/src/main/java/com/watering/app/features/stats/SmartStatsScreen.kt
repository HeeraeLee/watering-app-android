package com.watering.app.features.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.watering.app.R
import com.watering.app.core.model.DailyAchievement
import com.watering.app.core.service.TimeOfDayInsightResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartStatsScreen(
    onBack: () -> Unit,
    viewModel: SmartStatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.smart_stats_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.content_description_back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            item {
                SectionCard(title = stringResource(R.string.smart_stats_month_trend_title)) {
                    MonthBarChart(stats = uiState.monthStats)
                    Spacer(Modifier.height(16.dp))
                    InsightCard(insight = uiState.insight)
                    Spacer(Modifier.height(12.dp))
                    HydrationVolumeCard(volumeMl = uiState.todayHydrationVolumeMl)
                }
            }

            item {
                SectionCard(title = stringResource(R.string.smart_stats_annual_title)) {
                    AnnualHeatmap(days = uiState.annualDays)
                }
            }
        }
    }
}

@Composable
private fun MonthBarChart(stats: List<DayStat>) {
    if (stats.isEmpty()) return

    // 30개 막대는 화면 폭보다 넓어 가로 스크롤이 필요 — 가장 중요한 "오늘" 막대가
    // 오른쪽 끝에 있으므로 기본적으로 끝까지 스크롤된 상태로 보여준다
    val scrollState = rememberScrollState()
    LaunchedEffect(stats) {
        snapshotFlow { scrollState.maxValue }.collect { max ->
            if (max > 0) scrollState.scrollTo(max)
        }
    }

    Box(modifier = Modifier.horizontalScroll(scrollState)) {
        BarChartRow(
            entries = stats.map { day ->
                BarChartEntry(key = day.dateKey, count = day.count, goal = day.goal, isToday = day.isToday)
            },
            columnWidth = 12.dp,
            barWidth = 8.dp,
            barMaxHeight = 100.dp,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier
        )
    }
}

@Composable
private fun InsightCard(insight: TimeOfDayInsightResult) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("💡", fontSize = 20.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                text = when (insight) {
                    is TimeOfDayInsightResult.Found ->
                        stringResource(R.string.smart_stats_insight_template, insight.startHour, insight.endHour)
                    TimeOfDayInsightResult.NoClearPattern ->
                        stringResource(R.string.smart_stats_insight_no_pattern)
                    TimeOfDayInsightResult.InsufficientData ->
                        stringResource(R.string.smart_stats_insight_placeholder)
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun HydrationVolumeCard(volumeMl: Int) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🧪", fontSize = 20.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.smart_stats_hydration_volume, volumeMl),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun AnnualHeatmap(days: List<DailyAchievement?>) {
    if (days.isEmpty()) return

    // 오늘이 오른쪽 끝 열에 위치 — 기본적으로 끝까지 스크롤된 상태로 보여준다
    val scrollState = rememberScrollState()
    LaunchedEffect(days) {
        snapshotFlow { scrollState.maxValue }.collect { max ->
            if (max > 0) scrollState.scrollTo(max)
        }
    }

    val weeks = days.chunked(7)
    Row(
        modifier = Modifier.horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        weeks.forEach { week ->
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                week.forEach { day ->
                    val color = when {
                        day == null || day.totalCount <= 0 -> MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        day.isAchieved -> GreenColor
                        else -> AquaColor.copy(alpha = (0.3f + 0.7f * day.achievementRate).toFloat().coerceIn(0.3f, 1f))
                    }
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(color)
                    )
                }
            }
        }
    }
}
