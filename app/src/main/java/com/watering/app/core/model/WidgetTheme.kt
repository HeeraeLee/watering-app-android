package com.watering.app.core.model

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.watering.app.R
import kotlinx.serialization.Serializable

@Serializable
enum class WidgetTheme(@StringRes val displayNameRes: Int, val accentColor: Color) {
    DEFAULT(R.string.widget_theme_default, Color(0xFF00B4D8)),
    SUNSET(R.string.widget_theme_sunset, Color(0xFFFF7A59)),
    MONO(R.string.widget_theme_mono, Color(0xFF37474F)),
    NEON(R.string.widget_theme_neon, Color(0xFFFF2D95));

    companion object {
        // 목표 달성 색상은 테마와 무관하게 항상 고정 — 앱 쪽 Theme.kt와 동일한 골드로 통일
        // (그린 → 골드, 2026-07-04 사용자 피드백 반영)
        val ACHIEVED_COLOR = Color(0xFFF5C860)
    }
}
