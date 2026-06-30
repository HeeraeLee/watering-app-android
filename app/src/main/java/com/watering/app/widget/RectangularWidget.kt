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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.watering.app.MainActivity
import com.watering.app.R

private val DarkBg = Color(0xEE0D1B2A)
private val AquaColor = Color(0xFF00B4D8)
private val GreenColor = Color(0xFF34C759)

class RectangularWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = loadWidgetState(context)
        provideContent { RectangularWidgetContent(state) }
    }
}

class RectangularWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = RectangularWidget()
}

@Composable
private fun RectangularWidgetContent(state: WidgetState) {
    val context = LocalContext.current
    val size = LocalSize.current
    val isAchieved = state.achievementRate >= 1.0
    val accent = if (isAchieved) GreenColor else AquaColor
    val rate = state.achievementRate.coerceIn(0.0, 1.0).toFloat()
    val barWidth = (size.width - 32.dp) * rate
    val motivationText = when {
        isAchieved              -> "목표 달성! 🎉"
        state.achievementRate >= 0.7 -> "거의 다 왔어요!"
        state.achievementRate >= 0.3 -> "잘 하고 있어요"
        else                    -> "물 마실 시간이에요 💧"
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(DarkBg))
            .cornerRadius(20.dp)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java)))
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 타이틀 행
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_water_drop),
                    contentDescription = null,
                    modifier = GlanceModifier.size(13.dp),
                    colorFilter = ColorFilter.tint(ColorProvider(accent))
                )
                Spacer(GlanceModifier.width(4.dp))
                Text(
                    text = "워터링",
                    style = TextStyle(color = ColorProvider(accent), fontSize = 11.sp)
                )
            }
            Spacer(GlanceModifier.height(6.dp))
            // 잔 수 + 퍼센트 행
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "${state.totalCount}",
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(GlanceModifier.width(5.dp))
                Text(
                    text = "/ ${state.goal} 잔",
                    style = TextStyle(
                        color = ColorProvider(Color.White.copy(alpha = 0.5f)),
                        fontSize = 14.sp
                    )
                )
                Spacer(GlanceModifier.defaultWeight())
                Text(
                    text = "${(rate * 100).toInt()}%",
                    style = TextStyle(
                        color = ColorProvider(accent),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Spacer(GlanceModifier.height(10.dp))
            // 진행 바
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .cornerRadius(3.dp)
                    .background(ColorProvider(Color.White.copy(alpha = 0.15f)))
            ) {
                if (rate > 0f) {
                    Box(
                        modifier = GlanceModifier
                            .width(barWidth)
                            .fillMaxHeight()
                            .cornerRadius(3.dp)
                            .background(ColorProvider(accent))
                    ) {}
                }
            }
            Spacer(GlanceModifier.height(8.dp))
            Text(
                text = motivationText,
                style = TextStyle(
                    color = ColorProvider(Color.White.copy(alpha = 0.5f)),
                    fontSize = 11.sp
                )
            )
        }
    }
}
