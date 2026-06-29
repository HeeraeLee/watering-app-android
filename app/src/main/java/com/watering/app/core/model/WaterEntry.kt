package com.watering.app.core.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class WaterEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestampMillis: Long,   // epoch millis (LocalDateTime 직렬화 대신)
    val amount: Int,             // ml
    val drinkType: DrinkType
)
