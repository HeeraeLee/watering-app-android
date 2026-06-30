package com.watering.app.widget

import android.content.Context
import android.content.Intent
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
import androidx.glance.appwidget.action.actionStartActivity
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
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.watering.app.MainActivity
import com.watering.app.R

class NarrowWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = loadWidgetState(context)
        provideContent { NarrowWidgetContent(state) }
    }
}

class NarrowWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = NarrowWidget()
}

@Composable
private fun NarrowWidgetContent(state: WidgetState) {
    val context = LocalContext.current
    val size = LocalSize.current
    val isAchieved = state.achievementRate >= 1.0
    val accent = if (isAchieved) Color(0xFF34C759) else Color(0xFF00B4D8)
    val rate = state.achievementRate.coerceIn(0.0, 1.0).toFloat()
    // 고정 요소 너비 제외: 좌우padding(28) + 아이콘(16) + spacer(8) + 텍스트 추정(68) + spacer(10) + 퍼센트(36) + spacer(8) = 174dp
    val barTotal = (size.width - 174.dp).coerceAtLeast(8.dp)
    val barFill = (barTotal * rate).coerceAtLeast(0.dp)

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color(0xEE0D1B2A)))
            .cornerRadius(16.dp)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java)))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_water_drop),
            contentDescription = null,
            modifier = GlanceModifier.size(16.dp),
            colorFilter = ColorFilter.tint(ColorProvider(accent))
        )
        Spacer(GlanceModifier.width(8.dp))
        Text(
            text = "${state.totalCount} / ${state.goal}",
            style = TextStyle(
                color = ColorProvider(Color.White),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(GlanceModifier.width(10.dp))
        // 진행 바 (남은 공간 전체)
        Box(
            modifier = GlanceModifier
                .defaultWeight()
                .height(4.dp)
                .cornerRadius(2.dp)
                .background(ColorProvider(Color.White.copy(alpha = 0.15f)))
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
        Spacer(GlanceModifier.width(8.dp))
        Text(
            text = "${(rate * 100).toInt()}%",
            style = TextStyle(
                color = ColorProvider(accent),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}
