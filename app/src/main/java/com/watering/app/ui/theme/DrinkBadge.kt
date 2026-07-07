package com.watering.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.watering.app.R
import com.watering.app.core.model.DrinkType

private data class DrinkBadgeColors(val background: Color, val icon: Color)

// 음료 아이콘 원형 뱃지 색상 — 웹 목업 "① 파스텔 필" 안 확정 색상 (2026-07-07).
// 물은 앱 프라이머리 아쿠아(Theme.kt의 LightColorScheme/DarkColorScheme primaryContainer/onPrimaryContainer)와 동일한 값을 사용.
private fun drinkBadgeColors(type: DrinkType, dark: Boolean): DrinkBadgeColors = when (type) {
    DrinkType.WATER -> if (dark) DrinkBadgeColors(Color(0xFF004F62), Color(0xFFCAF0F8))
        else DrinkBadgeColors(Color(0xFFCAF0F8), Color(0xFF003544))
    DrinkType.COFFEE -> if (dark) DrinkBadgeColors(Color(0xFF4A3524), Color(0xFFE3B78E))
        else DrinkBadgeColors(Color(0xFFF0E1D3), Color(0xFF8A5A34))
    DrinkType.TEA -> if (dark) DrinkBadgeColors(Color(0xFF204334), Color(0xFF9BDCB4))
        else DrinkBadgeColors(Color(0xFFDCEEE0), Color(0xFF4C7A5D))
    DrinkType.JUICE -> if (dark) DrinkBadgeColors(Color(0xFF5A3D14), Color(0xFFFFC978))
        else DrinkBadgeColors(Color(0xFFFCE6C8), Color(0xFFC7791E))
    DrinkType.MILK -> if (dark) DrinkBadgeColors(Color(0xFF4A4432), Color(0xFFEFE6C9))
        else DrinkBadgeColors(Color(0xFFF2ECDC), Color(0xFF9C8459))
    DrinkType.OTHER -> if (dark) DrinkBadgeColors(Color(0xFF3A2C52), Color(0xFFCBB5EE))
        else DrinkBadgeColors(Color(0xFFEAE1F5), Color(0xFF7B5EA7))
}

private fun drinkIconRes(type: DrinkType): Int = when (type) {
    DrinkType.COFFEE -> R.drawable.ic_drink_coffee
    DrinkType.TEA -> R.drawable.ic_drink_tea
    DrinkType.JUICE -> R.drawable.ic_drink_juice
    DrinkType.MILK -> R.drawable.ic_drink_milk
    DrinkType.OTHER -> R.drawable.ic_drink_other
    DrinkType.WATER -> throw IllegalArgumentException("WATER는 Icons.Filled.WaterDrop을 직접 사용")
}

@Composable
fun DrinkBadge(type: DrinkType, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val colors = drinkBadgeColors(type, isSystemInDarkTheme())
    Surface(shape = CircleShape, color = colors.background, modifier = modifier.size(size)) {
        Box(contentAlignment = Alignment.Center) {
            if (type == DrinkType.WATER) {
                Icon(
                    imageVector = Icons.Filled.WaterDrop,
                    contentDescription = null,
                    tint = colors.icon,
                    modifier = Modifier.size(size * 0.5f)
                )
            } else {
                Icon(
                    painter = painterResource(drinkIconRes(type)),
                    contentDescription = null,
                    tint = colors.icon,
                    modifier = Modifier.size(size * 0.5f)
                )
            }
        }
    }
}
