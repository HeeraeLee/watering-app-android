package com.watering.app.widget

import android.content.Intent
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.watering.app.MainActivity

class RectangularWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val record = loadWidgetState(context)
        provideContent {
            RectangularWidgetContent(
                current = record.totalCount,
                goal = record.goal,
                rate = record.achievementRate
            )
        }
    }
}

class RectangularWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = RectangularWidget()
}

@Composable
private fun RectangularWidgetContent(current: Int, goal: Int, rate: Double) {
    val context = LocalContext.current
    val bgColor = Color(0xFF1C2A3A)
    val accentColor = when {
        rate >= 1.0 -> Color(0xFF4CAF50)
        rate >= 0.7 -> Color(0xFF00B4D8)
        rate >= 0.3 -> Color(0xFFFF9800)
        else        -> Color(0xFFF44336)
    }
    val motivationText = when {
        rate >= 1.0 -> "목표 달성! 🎉"
        rate >= 0.7 -> "거의 다 왔어요!"
        rate >= 0.3 -> "잘 하고 있어요"
        else        -> "물 마실 시간이에요 💧"
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(bgColor))
            .padding(12.dp)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$current / $goal 잔",
            style = TextStyle(
                color = ColorProvider(Color.White),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = "${(rate * 100).toInt()}%",
            style = TextStyle(
                color = ColorProvider(accentColor),
                fontSize = 14.sp
            )
        )
        Spacer(modifier = GlanceModifier.height(6.dp))
        Text(
            text = motivationText,
            style = TextStyle(
                color = ColorProvider(Color.White.copy(alpha = 0.7f)),
                fontSize = 12.sp
            )
        )
    }
}
