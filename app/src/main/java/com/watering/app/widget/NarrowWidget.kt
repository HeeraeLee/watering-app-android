package com.watering.app.widget

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.watering.app.R

// 잔수·퍼센트 텍스트를 고정 폭으로 둬야 barTotal 추정(size.width - 고정폭)이 자릿수와 무관하게 항상 정확하다
// (실측 176dp 기준, 막대가 너무 얇아지지 않도록 최소 여백으로 타이트하게 설정)
private val CountTextWidth = 46.dp
private val PercentTextWidth = 32.dp

class NarrowWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { NarrowWidgetContent(rememberWidgetState(context)) }
    }
}

class NarrowWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = NarrowWidget()
}

@Composable
private fun NarrowWidgetContent(state: WidgetState) {
    val context = LocalContext.current
    val size = LocalSize.current
    val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    val isAchieved = state.achievementRate >= 1.0
    val accent = if (isAchieved) Color(0xFF34C759) else Color(0xFF00B4D8)
    val rate = state.achievementRate.coerceIn(0.0, 1.0).toFloat()
    // 아이콘(16) + 여백(6+6+6) + 잔수 텍스트(46) + 퍼센트 텍스트(32) + 좌우 패딩(28) = 140dp
    // (막대 요청에 따라 여백을 8/10/8→6/6/6으로 줄여 막대 실제 공간을 확보 — 텍스트 폭은 그대로라 잘림 위험 없음)
    val barTotal = (size.width - 140.dp).coerceAtLeast(8.dp)
    val barFill = (barTotal * rate).coerceAtLeast(0.dp)

    val bgColor = if (isDark) Color(0xEE0D1B2A) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF0D1B2A)
    val barTrack = if (isDark) Color.White.copy(alpha = 0.15f) else accent.copy(alpha = 0.12f)

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(bgColor))
            .cornerRadius(16.dp)
            .clickable(actionRunCallback<AddWaterAction>())
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_water_drop),
            contentDescription = null,
            modifier = GlanceModifier.size(16.dp),
            colorFilter = ColorFilter.tint(ColorProvider(accent))
        )
        Spacer(GlanceModifier.width(6.dp))
        Text(
            text = "${state.totalCount} / ${state.goal}",
            modifier = GlanceModifier.width(CountTextWidth),
            style = TextStyle(
                color = ColorProvider(textColor),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(GlanceModifier.width(6.dp))
        Box(
            modifier = GlanceModifier
                .defaultWeight()
                .height(4.dp)
                .cornerRadius(2.dp)
                .background(ColorProvider(barTrack))
        ) {
            if (rate > 0f) {
                Box(
                    modifier = GlanceModifier
                        .width(barFill)
                        .fillMaxHeight()
                        .cornerRadius(2.dp)
                        .background(ColorProvider(accent))
                ) {}
            }
        }
        Spacer(GlanceModifier.width(6.dp))
        Text(
            text = "${(rate * 100).toInt()}%",
            modifier = GlanceModifier.width(PercentTextWidth),
            style = TextStyle(
                color = ColorProvider(accent),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.End
            )
        )
    }
}
