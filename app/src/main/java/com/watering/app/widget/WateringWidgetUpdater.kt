package com.watering.app.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WateringWidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun updateAll() = withContext(Dispatchers.Default) {
        val manager = GlanceAppWidgetManager(context)
        listOf(CircularWidget(), RectangularWidget(), NarrowWidget()).forEach { widget ->
            try {
                val ids = manager.getGlanceIds(widget::class.java)
                Log.d("WateringWidget", "updating ${widget::class.simpleName}: ${ids.size} instances")
                ids.forEach { id -> widget.update(context, id) }
            } catch (e: Exception) {
                Log.e("WateringWidget", "update failed for ${widget::class.simpleName}", e)
            }
        }
    }
}
