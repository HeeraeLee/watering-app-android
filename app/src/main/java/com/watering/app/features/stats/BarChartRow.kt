package com.watering.app.features.stats

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val AquaColor = Color(0xFF00B4D8)
val GreenColor = Color(0xFF34C759)

// WeekBarChart(7일, 라벨 있음)와 MonthBarChart(30일, 라벨 없음)가 공유하는 막대 드로잉 프리미티브.
data class BarChartEntry(
    val key: String,
    val count: Int,
    val goal: Int,
    val isToday: Boolean = false,
    val topLabel: String? = null,
    val bottomLabel: String? = null
)

@Composable
fun BarChartRow(
    entries: List<BarChartEntry>,
    columnWidth: Dp,
    barWidth: Dp,
    barMaxHeight: Dp = 140.dp,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.SpaceEvenly,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    if (entries.isEmpty()) return
    val maxCount = maxOf(entries.maxOf { it.count }, entries.first().goal, 1)

    Row(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.Bottom
    ) {
        entries.forEach { entry ->
            val fillRatio by animateFloatAsState(
                targetValue = (entry.count.toFloat() / maxCount).coerceIn(0f, 1f),
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "bar_${entry.key}"
            )
            val goalRatio = (entry.goal.toFloat() / maxCount).coerceIn(0f, 1f)
            val isAchieved = entry.count >= entry.goal
            val barColor = when {
                isAchieved -> GreenColor
                entry.isToday -> AquaColor
                else -> AquaColor.copy(alpha = 0.45f)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.width(columnWidth)
            ) {
                if (entry.topLabel != null) {
                    if (entry.count > 0) {
                        Text(
                            text = entry.topLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isAchieved) GreenColor else AquaColor,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(2.dp))
                    } else {
                        Spacer(Modifier.height(18.dp))
                    }
                }

                // 바 + 목표선
                Box(
                    modifier = Modifier
                        .width(barWidth)
                        .height(barMaxHeight),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    // 배경 트랙
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    )
                    // 채워진 바
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(barMaxHeight * fillRatio)
                            .clip(RoundedCornerShape(8.dp))
                            .background(barColor)
                    )
                    // 목표 기준선
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .align(Alignment.BottomCenter)
                            .padding(bottom = (barMaxHeight * goalRatio) - 1.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        )
                    }
                }

                if (entry.bottomLabel != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = entry.bottomLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (entry.isToday)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (entry.isToday) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
