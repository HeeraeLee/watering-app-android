package com.watering.app.widget

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
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
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.watering.app.R

private val CupWidth = 64.dp
private val CupHeight = 96.dp

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
    val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    val isAchieved = state.achievementRate >= 1.0
    val accent = state.theme.accentColor
    val rate = state.achievementRate.coerceIn(0.0, 1.0).toFloat()
    val motivationText = when {
        isAchieved                   -> context.getString(R.string.label_goal_achieved_banner)
        state.achievementRate >= 0.7 -> context.getString(R.string.widget_motivation_almost_there)
        state.achievementRate >= 0.3 -> context.getString(R.string.widget_motivation_keep_going)
        else                         -> context.getString(R.string.widget_motivation_time_to_drink)
    }

    // 목표 달성 시 다크모드 여부와 무관하게 테마 색 배경으로 반전 — 다크모드의 기존(미달성) 스타일은 그대로 유지
    val achievedText = readableTextColor(accent)
    val bgColor = if (isAchieved) accent else if (isDark) WidgetDarkBg else Color.White
    val primaryText = if (isAchieved) achievedText else if (isDark) Color.White else Color(0xFF0D1B2A)
    val secondaryText = if (isAchieved) achievedText.copy(alpha = 0.7f) else if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF888888)
    val foregroundAccent = if (isAchieved) achievedText else accent

    // 얇은 진행바 대신 컵 모양 그래픽 자체가 채워지는 형태 — Glance는 SVG/Canvas 클리핑을
    // Composable에서 직접 지원하지 않아 비트맵으로 미리 그려서 Image에 담아 넣는다
    val density = context.resources.displayMetrics.density
    val cupWidthPx = (CupWidth.value * density).toInt()
    val cupHeightPx = (CupHeight.value * density).toInt()
    val fillColorArgb = foregroundAccent.toArgb()
    val cupBitmap = remember(rate, fillColorArgb, cupWidthPx, cupHeightPx) {
        createCupBitmap(cupWidthPx, cupHeightPx, rate, fillColorArgb, fillColorArgb)
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(bgColor))
            .cornerRadius(20.dp)
            .semantics { contentDescription = context.getString(R.string.widget_talkback_hint, state.totalCount, state.goal) }
            .clickable(actionRunCallback<AddWaterAction>())
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize().padding(18.dp),
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
                Spacer(GlanceModifier.width(6.dp))
                Text(
                    text = context.getString(R.string.widget_total_ml_today, state.totalMl),
                    style = TextStyle(color = ColorProvider(foregroundAccent), fontSize = 15.sp)
                )
            }
            Spacer(GlanceModifier.defaultWeight())
            Box(
                modifier = GlanceModifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        provider = ImageProvider(cupBitmap),
                        contentDescription = null,
                        modifier = GlanceModifier.width(CupWidth).height(CupHeight)
                    )
                    Spacer(GlanceModifier.width(18.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${state.totalCount}",
                                style = TextStyle(
                                    color = ColorProvider(primaryText),
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(GlanceModifier.width(4.dp))
                            Text(
                                text = context.getString(R.string.glasses_with_slash, state.goal),
                                style = TextStyle(color = ColorProvider(secondaryText), fontSize = 16.sp)
                            )
                        }
                        Spacer(GlanceModifier.height(4.dp))
                        Text(
                            text = "${(rate * 100).toInt()}%",
                            style = TextStyle(
                                color = ColorProvider(foregroundAccent),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
            Spacer(GlanceModifier.defaultWeight())
            Text(
                text = motivationText,
                style = TextStyle(color = ColorProvider(secondaryText), fontSize = 14.sp, textAlign = TextAlign.Center),
                modifier = GlanceModifier.fillMaxWidth()
            )
        }
    }
}
