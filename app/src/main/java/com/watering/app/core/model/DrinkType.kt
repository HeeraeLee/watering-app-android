package com.watering.app.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class DrinkType(val labelResKey: String, val hydrationRate: Double) {
    WATER("drink_water", 1.0),
    COFFEE("drink_coffee", 0.7),
    TEA("drink_tea", 0.9),
    JUICE("drink_juice", 0.85),
    MILK("drink_milk", 0.88),
    OTHER("drink_other", 0.8)
}
