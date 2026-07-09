package com.watering.app.widget

import androidx.compose.ui.graphics.Color
import kotlin.math.pow

// 위젯 다크모드 기본 배경/텍스트 — Circular/Rectangular/Narrow 공통
val WidgetDarkBg = Color(0xEE0D1B2A)
val WidgetDarkText = Color(0xFF0D1B2A)

// 목표 달성 시 파스텔 테마 색 배경 위에서 WCAG 대비가 더 좋은 쪽(흰색/네이비)을 자동 선택
// (버터 옐로우 등 밝은 테마에서 흰 글자 대비가 1.4:1까지 떨어지던 문제 수정)
fun readableTextColor(background: Color): Color {
    val contrastWithDark = contrastRatio(WidgetDarkText, background)
    val contrastWithWhite = contrastRatio(Color.White, background)
    return if (contrastWithDark >= contrastWithWhite) WidgetDarkText else Color.White
}

private fun contrastRatio(a: Color, b: Color): Double {
    val l1 = relativeLuminance(a) + 0.05
    val l2 = relativeLuminance(b) + 0.05
    return if (l1 > l2) l1 / l2 else l2 / l1
}

private fun relativeLuminance(color: Color): Double {
    fun channel(v: Float): Double {
        val c = v.toDouble()
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
}
