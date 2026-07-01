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
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.watering.app.R

class CircularWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { CircularWidgetContent(rememberWidgetState(context)) }
    }
}

class CircularWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = CircularWidget()
}

@Composable
private fun CircularWidgetContent(state: WidgetState) {
    val context = LocalContext.current
    val size = LocalSize.current
    val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    val isAchieved = state.achievementRate >= 1.0
    val accent = if (isAchieved) Color(0xFF34C759) else Color(0xFF00B4D8)
    val rate = state.achievementRate.coerceIn(0.0, 1.0).toFloat()
    val barWidth = (size.width - 28.dp) * rate

    // 다크: 아쿠아/그린 배경 + 흰 텍스트 / 라이트: 흰 배경 + 아쿠아/그린 텍스트
    val bgColor = if (isDark) accent else Color.White
    val textColor = if (isDark) Color.White else accent
    val subTextColor = if (isDark) Color.White.copy(alpha = 0.75f) else Color(0xFF666666)
    val barTrackColor = if (isDark) Color.White.copy(alpha = 0.3f) else accent.copy(alpha = 0.15f)
    val barFillColor = if (isDark) Color.White else accent

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(bgColor))
            .cornerRadius(24.dp)
            .clickable(actionRunCallback<AddWaterAction>())
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_water_drop),
                contentDescription = null,
                modifier = GlanceModifier.size(22.dp),
                colorFilter = ColorFilter.tint(ColorProvider(textColor))
            )
            Spacer(GlanceModifier.height(4.dp))
            Text(
                text = "${state.totalCount}",
                style = TextStyle(
                    color = ColorProvider(textColor),
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "/ ${state.goal}잔",
                style = TextStyle(
                    color = ColorProvider(subTextColor),
                    fontSize = 13.sp
                )
            )
            Spacer(GlanceModifier.defaultWeight())
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .cornerRadius(2.dp)
                    .background(ColorProvider(barTrackColor))
            ) {
                if (rate > 0f) {
                    Box(
                        modifier = GlanceModifier
                            .width(barWidth)
                            .fillMaxHeight()
                            .cornerRadius(2.dp)
                            .background(ColorProvider(barFillColor))
                    ) {}
                }
            }
        }
    }
}
