package com.watering.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.watering.app.R

// 목표 설정 화면(온보딩/설정)의 "N잔에 해당해요" 보조 텍스트 — 온보딩·설정 화면 공용.
// bodySmall 회색 → bodyMedium 기본색으로 두 번 키워도 여전히 안 띈다는 피드백(2026-07-10)으로,
// 웹 목업 10안 비교 후 "옅은 아쿠아 틴트 카드"로 확정. 꽉 찬 캡슐 배지(다른 후보 C/D)는 탭 가능한
// 칩처럼 오인될 수 있어 제외 — 테두리+틴트 배경만으로 존재감을 주는 이 안을 채택.
// 라이트 모드 텍스트 색은 원시 primary(#5DCEE6)를 쓰면 같은 계열의 옅은 배경과 대비가 약해서,
// CTA 버튼 텍스트에 쓰던 AquaCtaContentColor(진남색)를 재사용 — 다크 모드는 기존처럼 primary 그대로 사용
@Composable
fun GoalGlassesEquivalentBadge(glasses: Int, modifier: Modifier = Modifier) {
    val isDark = isSystemInDarkTheme()
    val accent = MaterialTheme.colorScheme.primary
    val textColor = if (isDark) accent else AquaCtaContentColor

    Text(
        text = stringResource(R.string.onboarding_goal_glasses_equivalent, glasses),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        color = textColor,
        modifier = modifier
            .background(accent.copy(alpha = 0.16f), RoundedCornerShape(10.dp))
            .border(1.dp, accent, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}
