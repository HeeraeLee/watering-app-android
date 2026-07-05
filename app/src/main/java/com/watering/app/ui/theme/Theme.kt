package com.watering.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// 홈/통계/설정 화면 공통 배경 그라데이션 (스카이 블루 → 라벤더 퍼플, 웹 목업 비교 후 확정)
val AppBackgroundGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFD3ECF3), Color(0xFFE1D7F0))
)

// 파스텔 옵션 N 확정 적용 (#7CDAED 근처 변형 중 선택)
private val WateringAqua = Color(0xFF5DCEE6)
private val WateringAquaDark = Color(0xFF48CAE4)

private val LightColorScheme = lightColorScheme(
    primary = WateringAqua,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCAF0F8),
    onPrimaryContainer = Color(0xFF003544),
    secondary = Color(0xFFF5C860), // 목표 달성 색상 옵션 P (골드) 테스트 중
    onSecondary = Color.White,
    tertiary = Color(0xFFFFC078)
)

private val DarkColorScheme = darkColorScheme(
    primary = WateringAquaDark,
    onPrimary = Color(0xFF003544),
    primaryContainer = Color(0xFF004F62),
    onPrimaryContainer = Color(0xFFCAF0F8),
    secondary = Color(0xFFF5C860), // 라이트 모드와 동일한 골드로 통일 (그린 잔존 버그 수정)
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
