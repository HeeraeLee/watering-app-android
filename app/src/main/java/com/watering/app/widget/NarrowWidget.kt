package com.watering.app.widget

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
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

// 잔수·퍼센트 텍스트를 고정 폭으로 둬야 배경 채움 폭 계산이 자릿수와 무관하게 항상 정확하다
private val CountTextWidth = 64.dp
private val PercentTextWidth = 50.dp
// 이 폭 미만이면 퍼센트 텍스트를 숨김 (아이콘16+여백4+잔수64+퍼센트50+좌우패딩28=162dp가 실제 최소 필요 폭)
// 진행률 자체는 배경 채움으로 계속 보이므로 퍼센트를 숨겨도 정보 손실은 최소화됨
private val FullContentMinWidth = 162.dp

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
    val accent = state.theme.accentColor
    val rate = state.achievementRate.coerceIn(0.0, 1.0).toFloat()
    val showPercent = size.width >= FullContentMinWidth
    val fillWidth = (size.width * rate).coerceIn(0.dp, size.width)

    // 목표 달성 시 다크모드 여부와 무관하게 테마 색 배경으로 반전 — 다크모드의 기존(미달성) 스타일은 그대로 유지
    val achievedText = readableTextColor(accent)
    val baseBg = if (isDark) WidgetDarkBg else Color.White
    // 위젯 배경 자체를 진행률 채움으로 사용 — track(안 채워진 영역)과 fill(채워진 영역) 모두
    // accent를 baseBg와 블렌딩한 톤이라 다크모드/라이트모드 어디서든 textColor 대비가 유지됨
    val trackColor = if (isAchieved) accent else lerp(baseBg, accent, 0.12f)
    val fillColor = if (isAchieved) accent else lerp(baseBg, accent, 0.35f)
    val textColor = if (isAchieved) achievedText else if (isDark) Color.White else Color(0xFF0D1B2A)
    val foregroundAccent = if (isAchieved) achievedText else accent

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(trackColor))
            .cornerRadius(16.dp)
            .semantics { contentDescription = context.getString(R.string.widget_talkback_hint, state.totalCount, state.goal) }
            .clickable(actionRunCallback<AddWaterAction>())
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxHeight()
                .width(fillWidth)
                .background(ColorProvider(fillColor))
        ) {}

        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
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
                text = "${state.totalCount} / ${state.goal}",
                modifier = GlanceModifier.width(CountTextWidth),
                style = TextStyle(
                    color = ColorProvider(textColor),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(GlanceModifier.defaultWeight())
            if (showPercent) {
                Text(
                    text = "${(rate * 100).toInt()}%",
                    modifier = GlanceModifier.width(PercentTextWidth),
                    style = TextStyle(
                        color = ColorProvider(foregroundAccent),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.End
                    )
                )
            }
        }
    }
}
