package com.watering.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.watering.app.MainActivity
import androidx.glance.layout.Spacer
import androidx.glance.layout.width
import androidx.compose.ui.unit.dp

class NarrowWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val record = loadWidgetState(context)
        provideContent {
            NarrowWidgetContent(
                current = record.totalCount,
                goal = record.goal,
                rate = record.achievementRate
            )
        }
    }
}

class NarrowWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = NarrowWidget()
}

@Composable
private fun NarrowWidgetContent(current: Int, goal: Int, rate: Double) {
    val bgColor = Color(0xFF1C2A3A)
    val textColor = Color.White
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(bgColor, bgColor))
            .padding(horizontal = 8.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "💧 $current / $goal",
            style = TextStyle(
                color = ColorProvider(textColor, textColor),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        )
        Spacer(modifier = GlanceModifier.width(8.dp))
        Text(
            text = "${(rate * 100).toInt()}%",
            style = TextStyle(
                color = ColorProvider(textColor.copy(alpha = 0.7f), textColor.copy(alpha = 0.7f)),
                fontSize = 12.sp
            )
        )
    }
}
