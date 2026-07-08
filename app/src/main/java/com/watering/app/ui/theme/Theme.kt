package com.watering.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// 홈/통계/설정/앱 정보 화면 공통 배경 그라데이션 (라벤더 → 크림, 앱 정보 화면 꾸미기 웹 목업에서
// 마음에 든다는 피드백으로 2026-07-06 전체 배경으로 확정)
// 2026-07-06: 다크모드에서 텍스트가 흐려 보이는 문제 수정 — 이 카드/배경 색들이 하드코딩 라이트
// 값이라 다크모드에서도 안 바뀌는데, 텍스트는 MaterialTheme.colorScheme 기반이라 다크모드용 밝은
// 색으로 바뀌어 "밝은 글씨 on 밝은 배경"이 되던 문제. isSystemInDarkTheme()로 분기하는 커스텀
// getter로 바꿔 호출부 변경 없이 다크모드 전용 값을 제공
// 2026-07-06: 다크모드 배경/카드 색이 파스텔을 단순히 어둡게만 낮춰서 칙칙해 보인다는 피드백으로,
// 채도를 살린 딥 퍼플 톤으로 재조정했다가(웹 목업 "① 채도를 살린 딥 퍼플"), 그마저도 칙칙하다는
// 추가 피드백으로 딥 틸 톤으로 최종 변경(웹 목업 다크 박스 색 10안 중 "① 딥 틸" 채택)
val AppBackgroundGradient: Brush
    @Composable get() = if (isSystemInDarkTheme()) {
        Brush.linearGradient(colors = listOf(Color(0xFF1A3D3A), Color(0xFF101E1C)))
    } else {
        Brush.linearGradient(colors = listOf(Color(0xFFF3EDFC), Color(0xFFFBF7F2)))
    }

// 홈 화면의 연속 기록/기록 카드 등에 쓰던 연한 크림 배경 (웹 목업 비교 후 확정), 통계 화면 카드에도 공용
val AppCardBackgroundColor: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF1C4D49) else Color(0xFFFDFAF4)

// 설정 화면의 안내 배너(건강 연동/백업) 공용 배경 (웹 목업 비교 후 소프트 옐로우로 확정)
// 2026-07-08: 다크모드 값이 옐로우를 단순히 어둡게 낮춘 올리브-갈색이라 딥 틸 다크 팔레트와 충돌한다는
// 피드백(Fable UI 리뷰)으로, 웹 목업 13안 비교 후 "틸 채우기 + 골드 보더"로 변경 — 배경은
// AppCardBackgroundColor와 통일해 칙칙함을 없애고, 라이트 모드의 옐로우 정체성은
// AppInfoBannerBorderColor(골드 보더)로만 다크 모드에 이어감. 라이트 모드는 변경 없음(무보더 유지)
val AppInfoBannerBackgroundColor: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF1C4D49) else Color(0xFFFBF3DA)

val AppInfoBannerBorderColor: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFFF5C860) else Color.Transparent

// 홈 화면 "마지막 취소" 텍스트 색 (Fable UI 리뷰 화면별 findings — 홈)
// 다크모드에서 기본 M3 error 색이 저대비로 보인다는 지적으로, 웹 목업 비교 후 소프트 로즈로 확정
// (2026-07-08). 라이트 모드는 기존과 동일하게 테마 error 색 그대로 사용.
val AppUndoTextColor: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFFFFB4AB) else MaterialTheme.colorScheme.error

// 파스텔 옵션 N 확정 적용 (#7CDAED 근처 변형 중 선택)
private val WateringAqua = Color(0xFF5DCEE6)
private val WateringAquaDark = Color(0xFF48CAE4)

// 설정 화면 "더보기" 메뉴 행 아이콘 타일 배경/아이콘 색 (Fable UI 리뷰 화면별 findings — 설정, 2026-07-08)
// 다크모드에서 라이트 전용 밝은 회백색 타일(#ECECEF)이 딥 틸 배경 위에서 튀어 보인다는 지적으로,
// 웹 목업 4안 비교 후 "프라이머리 틴트"(다크 프라이머리 16% 반투명 배경 + 다크 프라이머리 아이콘)로 확정.
// 라이트 모드는 기존 값 유지.
val SettingsMenuTileBackgroundColor: Color
    @Composable get() = if (isSystemInDarkTheme()) WateringAquaDark.copy(alpha = 0.16f) else Color(0xFFECECEF)

val SettingsMenuTileIconColor: Color
    @Composable get() = if (isSystemInDarkTheme()) WateringAquaDark else Color(0xFF5A5A66)

// 아쿠아(primary) 배경 CTA 버튼의 텍스트/아이콘 색 (Fable UI 리뷰 2026-07-08 Top 5 #3)
// 라이트 기본 onPrimary(흰색)는 파스텔 아쿠아 위에서 약 1.8:1로 WCAG AA(4.5:1) 미달이라,
// 다크 모드 onPrimary와 동일한 진한 남색을 양 모드 공통으로 사용 — 라이트 약 7.2:1, 다크 약 6.8:1.
// LightColorScheme.onPrimary 자체를 바꾸지 않는 이유: onPrimary를 암묵적으로 쓰는 다른 M3 컴포넌트
// (설정 화면 Switch 체크 썸 등)에 의도치 않은 회귀가 생기지 않도록 대상 CTA 버튼에만 개별 적용
val AquaCtaContentColor = Color(0xFF003544)

// 통계 화면 요약 칩(일평균/목표일/주간합) 값 텍스트 색 (Fable UI 리뷰 화면별 findings — 통계)
// 라이트 모드에서 프라이머리 아쿠아 그대로 쓰면 칩 배경(옅은 회색)과 저대비라 CTA와 동일한 남색으로
// 교체(2026-07-08). 다크모드는 기존 프라이머리(밝은 아쿠아)가 이미 충분한 대비라 변경하지 않음.
val AppSummaryValueColor: Color
    @Composable get() = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else AquaCtaContentColor

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
