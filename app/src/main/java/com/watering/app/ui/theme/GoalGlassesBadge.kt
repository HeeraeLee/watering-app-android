package com.watering.app.ui.theme

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.watering.app.R

// 목표 설정 화면(온보딩/설정)의 "N잔에 해당해요" 보조 텍스트 — 온보딩·설정 화면 공용.
// bodySmall 회색 → bodyMedium 기본색 → 옅은 아쿠아 틴트 카드까지 거쳐도 여전히 안 띈다는
// 피드백(2026-07-10)으로, 앱 물방울 아이콘(ic_water_drop) + 텍스트 조합으로 최종 확정
// (웹 목업 재비교 "B"안 채택 — 온보딩에서 먼저 쓰던 스타일을 설정 화면에도 통일 적용).
// 라이트 모드 텍스트 색은 "하루 목표" 큰 숫자(ml)와 같은 하늘색으로 통일해달라는 요청으로
// AquaCtaContentColor(진남색) 대신 primary 그대로 사용 — 다크 모드도 이미 primary라 결과적으로
// 라이트/다크 모두 큰 ml 숫자와 완전히 같은 색을 씀(2026-07-10)
@Composable
fun GoalGlassesEquivalentRow(glasses: Int, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(R.drawable.ic_water_drop),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.onboarding_goal_glasses_equivalent, glasses),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}
