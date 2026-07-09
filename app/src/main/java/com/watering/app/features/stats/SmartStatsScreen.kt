package com.watering.app.features.stats

import android.text.format.DateFormat
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
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.watering.app.R
import com.watering.app.core.model.DailyAchievement
import com.watering.app.core.service.TimeOfDayInsightResult
import com.watering.app.ui.theme.AppBackgroundGradient
import com.watering.app.ui.theme.AppCardBackgroundColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartStatsScreen(
    onBack: () -> Unit,
    viewModel: SmartStatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf(
        stringResource(R.string.smart_stats_month_trend_title),
        stringResource(R.string.smart_stats_annual_title)
    )

    Box(modifier = Modifier.fillMaxSize().background(AppBackgroundGradient)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(stringResource(R.string.smart_stats_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
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
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (selectedTab) {
                        0 -> item {
                            SectionCard(title = stringResource(R.string.smart_stats_month_trend_title)) {
                                MonthBarChart(stats = uiState.monthStats)
                                Spacer(Modifier.height(16.dp))
                                InsightCard(insight = uiState.insight)
                                Spacer(Modifier.height(12.dp))
                                HydrationVolumeCard(volumeMl = uiState.todayHydrationVolumeMl)
                            }
                        }
                        1 -> item {
                            SectionCard(title = stringResource(R.string.smart_stats_annual_title)) {
                                AnnualHeatmap(days = uiState.annualDays)
                                Spacer(Modifier.height(12.dp))
                                HeatmapLegend()
                            }
                        }
                    }
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
            columnWidth = 16.dp,
            barWidth = 11.dp,
            barMaxHeight = 130.dp,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
        )
    }

    // "6/8" 형식이 잔 수(예: "6/8잔")와 혼동된다는 지적(Fable UI 리뷰 화면별 findings — 스마트 통계)으로
    // 슬래시 없는 로케일별 날짜 형식으로 변경(한국어 "6월 8일", 영어 "Jun 8")
    val locale = Locale.getDefault()
    val formatter = DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "MMMd"), locale)
    val startLabel = LocalDate.parse(stats.first().dateKey).format(formatter)
    val todayLabel = stringResource(R.string.smart_stats_axis_today)
    Spacer(Modifier.height(6.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(startLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(todayLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun InsightCard(insight: TimeOfDayInsightResult) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = AppCardBackgroundColor,
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
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun HydrationVolumeCard(volumeMl: Int) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = AppCardBackgroundColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 시험관 이모지가 앱 아이콘 스타일과 안 어울린다는 지적으로 컵 벡터 아이콘으로 교체
            // (Fable UI 리뷰 화면별 findings — 스마트 통계, 2026-07-08)
            Icon(
                imageVector = Icons.Filled.LocalDrink,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.smart_stats_hydration_volume, volumeMl),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
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

    // 월/요일 라벨 추가 (Fable UI 리뷰 화면별 findings — 스마트 통계, 2026-07-08). `days`의 마지막
    // 항목은 ViewModel에서 항상 "오늘"로 채워지므로(null 아님), 그 dateKey를 기준으로 나머지 364개의
    // 실제 날짜를 역산한다 — null인 날도 위치(index)로 날짜를 알 수 있어 별도 상태 변경 없이 해결.
    val referenceDate = LocalDate.parse(days.last()!!.dateKey)
    val firstDate = referenceDate.minusDays((days.size - 1).toLong())
    val weekdayLabels = stringArrayResource(R.array.weekday_labels_short)
    val monthFormatter = remember { DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(Locale.getDefault(), "MMM"), Locale.getDefault()) }

    val weeks = days.chunked(7)

    Row {
        // 요일 라벨(고정, 스크롤 안 됨) — 월 라벨 행 높이만큼 상단 여백을 맞춘다
        Column(
            modifier = Modifier.padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            (0 until 7).forEach { row ->
                val weekday = firstDate.plusDays(row.toLong()).dayOfWeek.value % 7
                Box(modifier = Modifier.size(width = 16.dp, height = 12.dp), contentAlignment = Alignment.CenterStart) {
                    Text(weekdayLabels[weekday], style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Column(modifier = Modifier.horizontalScroll(scrollState)) {
            // 월 라벨 — 이전 주와 월이 다를 때만 표시(같은 월이 반복 표시되지 않도록)
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                var lastShownMonth = -1
                weeks.forEachIndexed { weekIndex, _ ->
                    val weekStartDate = firstDate.plusDays((weekIndex * 7).toLong())
                    val showLabel = weekStartDate.monthValue != lastShownMonth
                    if (showLabel) lastShownMonth = weekStartDate.monthValue
                    Box(modifier = Modifier.width(12.dp), contentAlignment = Alignment.CenterStart) {
                        if (showLabel) {
                            Text(
                                weekStartDate.format(monthFormatter),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                weeks.forEach { week ->
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        week.forEach { day ->
                            val color = when {
                                // alpha 0.15는 다크모드 카드 배경(#1C4D49) 위에서 대비 1.18:1로 격자가
                                // 거의 안 보이던 문제(2026-07-09) — BarChartRow의 배경 트랙과 동일한
                                // 0.35로 통일
                                day == null || day.totalCount <= 0 -> MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
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
    }
}

@Composable
private fun HeatmapLegend() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        LegendSwatch(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        Text(
            stringResource(R.string.smart_stats_legend_none),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(12.dp))
        LegendSwatch(color = AquaColor.copy(alpha = 0.6f))
        Text(
            stringResource(R.string.smart_stats_legend_partial),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(12.dp))
        LegendSwatch(color = GreenColor)
        Text(
            stringResource(R.string.smart_stats_legend_achieved),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LegendSwatch(color: Color) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(color)
    )
    Spacer(Modifier.width(4.dp))
}
