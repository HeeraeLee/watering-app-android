package com.watering.app.widget

import android.content.Context
import com.watering.app.core.data.WaterRepository
import com.watering.app.core.model.DayRecord
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun waterRepository(): WaterRepository
}

suspend fun loadWidgetState(context: Context): DayRecord {
    val entryPoint = EntryPointAccessors.fromApplication(
        context.applicationContext,
        WidgetEntryPoint::class.java
    )
    return entryPoint.waterRepository().todayRecord.first()
}
