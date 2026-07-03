package com.watering.app.core.model

import androidx.annotation.StringRes
import com.watering.app.R
import kotlinx.serialization.Serializable

@Serializable
enum class DrinkType(@StringRes val displayNameRes: Int, val hydrationRate: Double) {
    WATER(R.string.drink_water, 1.0),
    COFFEE(R.string.drink_coffee, 0.7),
    TEA(R.string.drink_tea, 0.9),
    JUICE(R.string.drink_juice, 0.85),
    MILK(R.string.drink_milk, 0.88),
    OTHER(R.string.drink_other, 0.8);

    val emoji: String get() = when (this) {
        WATER -> "💧"
        COFFEE -> "☕"
        TEA -> "🍵"
        JUICE -> "🧃"
        MILK -> "🥛"
        OTHER -> "🫗"
    }
}
