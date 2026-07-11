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
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.watering.app.R

// 이 크기에서 아이콘+숫자가 여유 있게 맞도록 디자인됨(WidgetThemeScreen.kt 미리보기와 동일 기준) —
// 실제 배정 크기가 이보다 작거나 크면 scale 비율만큼 아이콘·폰트·패딩을 전부 같은 비율로 조정
private val ReferenceWidth = 80.dp
private val ReferenceHeight = 94.dp

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
    val accent = state.theme.accentColor
    val rate = state.achievementRate.coerceIn(0.0, 1.0).toFloat()
    val fillHeight = (size.height * rate).coerceIn(0.dp, size.height)

    // 목표 달성 시 다크모드 여부와 무관하게 테마 색 배경으로 반전 — 다크모드의 기존(미달성) 스타일은 그대로 유지.
    // 위젯 배경 자체를 진행률 채움(아래→위, "컵에 물 차오르는" 느낌)으로 사용 —
    // track/fill 모두 accent를 baseBg와 블렌딩한 톤이라 다크모드에서도 텍스트 대비가 유지됨
    val achievedText = readableTextColor(accent)
    val baseBg = if (isDark) WidgetDarkBg else Color.White
    val trackColor = if (isAchieved) accent else lerp(baseBg, accent, 0.12f)
    val fillColor = if (isAchieved) accent else lerp(baseBg, accent, 0.35f)
    val textColor = if (isAchieved) achievedText else accent

    // 기종/런처 그리드에 따라 1x1에 실제 배정되는 크기가 제각각이라 아이콘·폰트·패딩을 전부
    // 실측 크기 비율로 스케일링 — RectangularWidget.kt와 동일한 접근(요소를 숨기지 않고
    // 항상 같은 비율로 축소/확대해 어떤 크기가 와도 잘리지 않게 함)
    val scale = minOf(
        size.width.value / ReferenceWidth.value,
        size.height.value / ReferenceHeight.value
    ).coerceIn(0.45f, 1.2f)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(trackColor))
            .cornerRadius(24.dp)
            .semantics { contentDescription = context.getString(R.string.widget_talkback_hint, state.totalCount, state.goal) }
            .clickable(actionRunCallback<AddWaterAction>())
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Spacer(GlanceModifier.height((size.height - fillHeight).coerceAtLeast(0.dp)))
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(fillHeight)
                    .background(ColorProvider(fillColor))
            ) {}
        }

        Column(
            modifier = GlanceModifier.fillMaxSize().padding(10.dp * scale),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_water_drop),
                contentDescription = null,
                modifier = GlanceModifier.size(22.dp * scale),
                colorFilter = ColorFilter.tint(ColorProvider(textColor))
            )
            Spacer(GlanceModifier.height(4.dp * scale))
            Text(
                text = "${state.totalCount}",
                style = TextStyle(
                    color = ColorProvider(textColor),
                    fontSize = (34f * scale).sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}
