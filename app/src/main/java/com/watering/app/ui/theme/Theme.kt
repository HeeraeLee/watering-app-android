package com.watering.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val WateringAqua = Color(0xFF00B4D8)
private val WateringAquaDark = Color(0xFF48CAE4)

private val LightColorScheme = lightColorScheme(
    primary = WateringAqua,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCAF0F8),
    onPrimaryContainer = Color(0xFF003544),
    secondary = Color(0xFF52B788),
    onSecondary = Color.White,
    tertiary = Color(0xFFFF9800)
)

private val DarkColorScheme = darkColorScheme(
    primary = WateringAquaDark,
    onPrimary = Color(0xFF003544),
    primaryContainer = Color(0xFF004F62),
    onPrimaryContainer = Color(0xFFCAF0F8),
    secondary = Color(0xFF74C69D),
    tertiary = Color(0xFFFFB74D)
)

@Composable
fun WateringTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
