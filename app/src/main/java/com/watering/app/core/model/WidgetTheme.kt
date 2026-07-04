package com.watering.app.core.model

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.watering.app.R
import kotlinx.serialization.Serializable

@Serializable
enum class WidgetTheme(@StringRes val displayNameRes: Int, val accentColor: Color) {
    DEFAULT(R.string.widget_theme_aqua, Color(0xFF7DD8E8)),
    MINT(R.string.widget_theme_mint, Color(0xFF8FE0C4)),
    LAVENDER(R.string.widget_theme_lavender, Color(0xFFB8A8E8)),
    CORAL(R.string.widget_theme_coral, Color(0xFFFFB08A)),
    ROSE_PINK(R.string.widget_theme_rose_pink, Color(0xFFF5A8C0)),
    SKY_BLUE(R.string.widget_theme_sky_blue, Color(0xFF8FBCE8)),
    BUTTER_YELLOW(R.string.widget_theme_butter_yellow, Color(0xFFF5DA8A)),
    GRAY_MONO(R.string.widget_theme_gray_mono, Color(0xFFB8C0C6))
}
