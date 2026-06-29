package com.watering.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val WateringBlue = Color(0xFF007AFF)
private val WateringBlueDark = Color(0xFF4DA6FF)

private val LightColorScheme = lightColorScheme(
    primary = WateringBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E8FF),
    secondary = Color(0xFF4CAF50),
    tertiary = Color(0xFFFF9800)
)

private val DarkColorScheme = darkColorScheme(
    primary = WateringBlueDark,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF003D7A),
    secondary = Color(0xFF81C784),
    tertiary = Color(0xFFFFB74D)
)

@Composable
fun WateringTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Android 12+: Dynamic Color (Material You) 적용
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
