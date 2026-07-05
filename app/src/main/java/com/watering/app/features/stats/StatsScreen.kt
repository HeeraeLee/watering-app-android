package com.watering.app.features.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.watering.app.R
import com.watering.app.ui.theme.AppBackgroundGradient
import com.watering.app.ui.theme.AppCardBackgroundColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    onNavigateToSmartStats: () -> Unit = {},
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize().background(AppBackgroundGradient)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(stringResource(R.string.stats_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.content_description_back))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // 이번 주 바 차트
            item {
                SectionCard(title = stringResource(R.string.stats_this_week_record)) {
                    WeekBarChart(stats = uiState.weekStats)
                }
            }

            // 주간 요약 카드 3개
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryChip(
                        label = stringResource(R.string.stats_weekly_avg),
                        value = stringResource(R.string.stats_weekly_avg_value, uiState.weeklyAvg),
                        modifier = Modifier.weight(1f)
                    )
                    SummaryChip(
                        label = stringResource(R.string.stats_goal_days),
                        value = stringResource(R.string.stats_goal_days_value, uiState.goalDays),
                        modifier = Modifier.weight(1f)
                    )
                    SummaryChip(
                        label = stringResource(R.string.stats_weekly_total),
                        value = stringResource(R.string.stats_weekly_total_value, uiState.weeklyTotal),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 연속 기록
            item {
                SectionCard(title = stringResource(R.string.stats_streak_section)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StreakStatCard(
                            icon = Icons.Filled.LocalFireDepartment,
                            tint = MaterialTheme.colorScheme.tertiary,
                            label = stringResource(R.string.label_current_streak),
                            value = stringResource(R.string.streak_days_value, uiState.currentStreak),
                            modifier = Modifier.weight(1f)
                        )
                        StreakStatCard(
                            icon = Icons.Filled.EmojiEvents,
                            tint = MaterialTheme.colorScheme.secondary,
                            label = stringResource(R.string.label_longest_streak),
                            value = stringResource(R.string.streak_days_value, uiState.longestStreak),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 스마트 통계 (30일 트렌드 + 연간 기록)
            item {
                SmartStatsCta(onNavigateToSmartStats = onNavigateToSmartStats)
            }
        }
        }
    }
}

@Composable
private fun SmartStatsCta(onNavigateToSmartStats: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onNavigateToSmartStats)
            .background(AppCardBackgroundColor, RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(R.string.smart_stats_title),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                stringResource(R.string.stats_smart_cta_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SectionCard(title: String, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = AppCardBackgroundColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun WeekBarChart(stats: List<DayStat>) {
    if (stats.isEmpty()) return

    BarChartRow(
        entries = stats.map { day ->
            BarChartEntry(
                key = day.dateKey,
                count = day.count,
                goal = day.goal,
                isToday = day.isToday,
                topLabel = "${day.count}",
                bottomLabel = day.label
            )
        },
        columnWidth = 36.dp,
        barWidth = 28.dp
    )

    Spacer(Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.stats_goal_line, stats.firstOrNull()?.goal ?: 0),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.End
    )
}

@Composable
private fun SummaryChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StreakStatCard(icon: ImageVector, tint: Color, label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
