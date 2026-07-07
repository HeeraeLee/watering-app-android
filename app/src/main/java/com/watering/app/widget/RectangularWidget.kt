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
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.watering.app.R

private val DarkBg = Color(0xEE0D1B2A)

class RectangularWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { RectangularWidgetContent(rememberWidgetState(context)) }
    }
}

class RectangularWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = RectangularWidget()
}

@Composable
private fun RectangularWidgetContent(state: WidgetState) {
    val context = LocalContext.current
    val size = LocalSize.current
    val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    val isAchieved = state.achievementRate >= 1.0
    val accent = state.theme.accentColor
    val rate = state.achievementRate.coerceIn(0.0, 1.0).toFloat()
    val barWidth = (size.width - 32.dp) * rate
    val motivationText = when {
        isAchieved                   -> context.getString(R.string.label_goal_achieved_banner)
        state.achievementRate >= 0.7 -> context.getString(R.string.widget_motivation_almost_there)
        state.achievementRate >= 0.3 -> context.getString(R.string.widget_motivation_keep_going)
        else                         -> context.getString(R.string.widget_motivation_time_to_drink)
    }

    // 목표 달성 시 다크모드 여부와 무관하게 테마 색 배경 + 흰 텍스트로 반전 — 다크모드의 기존(미달성) 스타일은 그대로 유지
    val bgColor = if (isAchieved) accent else if (isDark) DarkBg else Color.White
    val primaryText = if (isAchieved || isDark) Color.White else Color(0xFF0D1B2A)
    val secondaryText = if (isAchieved) Color.White.copy(alpha = 0.7f) else if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF888888)
    val barTrack = if (isAchieved) Color.White.copy(alpha = 0.3f) else if (isDark) Color.White.copy(alpha = 0.15f) else accent.copy(alpha = 0.12f)
    val foregroundAccent = if (isAchieved) Color.White else accent

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(bgColor))
            .cornerRadius(20.dp)
            .semantics { contentDescription = context.getString(R.string.widget_talkback_hint, state.totalCount, state.goal) }
            .clickable(actionRunCallback<AddWaterAction>())
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_water_drop),
                    contentDescription = null,
                    modifier = GlanceModifier.size(16.dp),
                    colorFilter = ColorFilter.tint(ColorProvider(foregroundAccent))
                )
                Spacer(GlanceModifier.width(4.dp))
                Text(
                    text = context.getString(R.string.widget_app_label),
                    style = TextStyle(color = ColorProvider(foregroundAccent), fontSize = 13.sp)
                )
            }
            Spacer(GlanceModifier.height(6.dp))
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "${state.totalCount}",
                    style = TextStyle(
                        color = ColorProvider(primaryText),
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(GlanceModifier.width(5.dp))
                Text(
                    text = context.getString(R.string.glasses_with_slash, state.goal),
                    style = TextStyle(color = ColorProvider(secondaryText), fontSize = 14.sp)
                )
                Spacer(GlanceModifier.defaultWeight())
                Text(
                    text = "${(rate * 100).toInt()}%",
                    style = TextStyle(
                        color = ColorProvider(foregroundAccent),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Spacer(GlanceModifier.height(10.dp))
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .cornerRadius(3.dp)
                    .background(ColorProvider(barTrack))
            ) {
                if (rate > 0f) {
                    Box(
                        modifier = GlanceModifier
                            .width(barWidth)
                            .fillMaxHeight()
                            .cornerRadius(3.dp)
                            .background(ColorProvider(foregroundAccent))
                    ) {}
                }
            }
            Spacer(GlanceModifier.height(8.dp))
            Text(
                text = motivationText,
                style = TextStyle(color = ColorProvider(secondaryText), fontSize = 13.sp)
            )
        }
    }
}
