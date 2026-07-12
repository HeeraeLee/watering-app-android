package com.watering.app.features.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.watering.app.R
import com.watering.app.core.model.WidgetTheme
import com.watering.app.ui.theme.AppBackgroundGradient
import com.watering.app.ui.theme.WidgetPreviewCardBackgroundColor
import com.watering.app.ui.theme.WidgetPreviewCardBorderColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetThemeScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    // 스와치 탭은 이 화면 안에서만 값을 임시로 들고 있다가(draftTheme) "적용하기"를 눌러야
    // viewModel.updateWidgetTheme()가 호출돼 실제 위젯에 반영됨(2026-07-10, 즉시 적용 → 지연 적용).
    // 화면을 벗어나면 이 상태는 자동 소멸하므로 미적용 선택은 별도 처리 없이 조용히 폐기됨.
    var draftTheme by remember(settings.widgetTheme) { mutableStateOf(settings.widgetTheme) }
    val hasPendingChange = draftTheme != settings.widgetTheme

    Box(modifier = Modifier.fillMaxSize().background(AppBackgroundGradient)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.settings_section_widget_theme)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.content_description_back))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            bottomBar = {
                WidgetThemeApplyBar(
                    enabled = hasPendingChange,
                    onApply = { viewModel.updateWidgetTheme(draftTheme) }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                WidgetThemeSetting(
                    draftTheme = draftTheme,
                    onDraftThemeChange = { draftTheme = it }
                )
            }
        }
    }
}

// 3버튼 내비게이션 기기에서 버튼이 시스템 내비게이션 바에 가려 탭이 안 먹히던 문제로
// navigationBarsPadding 추가(2026-07-10, 실기기 확인)
@Composable
private fun WidgetThemeApplyBar(enabled: Boolean, onApply: () -> Unit) {
    Button(
        onClick = onApply,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(stringResource(R.string.widget_theme_apply_button))
    }
}

// 색상 스와치 선택 링 색 (Fable UI 리뷰 findings — 위젯 테마, 2026-07-08)
// 라이트 모드에서 onSurface(순검정) 링이 파스텔 스와치와 이질적으로 튄다는 지적으로, 웹 목업 비교
// 후 "동색 계열을 진하게"(선택된 색상 자체를 어둡게 만든 톤) 채택. RGB를 곱연산으로 어둡게 만들어
// 색조는 유지하면서 명도만 낮춤 — 색상 팔레트가 늘어나도 별도 매핑 없이 자동으로 적용됨
private fun Color.ringColor(): Color = copy(
    red = red * 0.55f,
    green = green * 0.55f,
    blue = blue * 0.55f,
    alpha = 1f
)

// 위젯 테마 화면 미리보기 종류 선택 (Fable UI 리뷰 findings — 위젯 테마)
// 기존엔 아이콘+숫자만 있는 단순 미리보기 하나만 있어 정보가 부족하고 화면 하단이 비어있다는
// 지적으로, 웹 목업 비교 후 "위젯 3종을 탭으로 전환하며 미리보기" 안 채택
private enum class WidgetPreviewShape(@StringRes val labelRes: Int) {
    MINI(R.string.widget_shape_mini),
    CARD(R.string.widget_shape_card),
    WIDE(R.string.widget_shape_wide)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WidgetThemeSetting(
    draftTheme: WidgetTheme,
    onDraftThemeChange: (WidgetTheme) -> Unit
) {
    var previewShape by remember { mutableStateOf(WidgetPreviewShape.MINI) }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            stringResource(R.string.settings_widget_theme_guide),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WidgetTheme.entries.forEach { theme ->
                val isSelected = theme == draftTheme
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(theme.accentColor)
                        .border(
                            width = if (isSelected) 3.dp else 0.dp,
                            color = theme.accentColor.ringColor(),
                            shape = CircleShape
                        )
                        .clickable { onDraftThemeChange(theme) }
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WidgetPreviewShape.entries.forEach { shape ->
                FilterChip(
                    selected = shape == previewShape,
                    onClick = { previewShape = shape },
                    label = { Text(stringResource(shape.labelRes)) },
                    colors = selectedChipColors()
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        WidgetPreviewCard(color = draftTheme.accentColor, shape = previewShape)
        Spacer(Modifier.height(16.dp))
    }
}

// 웹 목업 6안 중 "화이트 카드 + 점선 보더" 채택(2026-07-12) — Compose의 Modifier.border는
// 실선만 지원해 drawBehind로 직접 점선 사각형을 그림
private fun Modifier.dashedBorder(color: Color, cornerRadius: Dp, strokeWidth: Dp = 1.5.dp) = drawBehind {
    drawRoundRect(
        color = color,
        style = Stroke(
            width = strokeWidth.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
        ),
        cornerRadius = CornerRadius(cornerRadius.toPx())
    )
}

@Composable
private fun WidgetPreviewCard(color: Color, shape: WidgetPreviewShape) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = WidgetPreviewCardBackgroundColor,
        modifier = Modifier
            .fillMaxWidth()
            .dashedBorder(WidgetPreviewCardBorderColor, 20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (shape) {
                WidgetPreviewShape.MINI -> MiniWidgetPreview(color)
                WidgetPreviewShape.CARD -> CardWidgetPreview(color)
                WidgetPreviewShape.WIDE -> WideWidgetPreview(color)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.settings_widget_theme_preview_label, stringResource(shape.labelRes)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// 크기·모서리 반경은 실기기(Galaxy A25, One UI 1x1 셀) 실측값 기준 — 완전한 원이 아니라
// 둥근 사각형(스퀴클)에 가까움. cornerRadius 24.dp는 CircularWidget.kt와 동일한 값을 그대로 사용
//
// 2026-07-11 리디자인: 얇은 가로 바 대신 배경 자체가 진행률만큼 아래→위로 채워지는 CircularWidget.kt의
// 실제 렌더링(세로 채움, track/fill 모두 accent를 배경색과 블렌딩한 톤)과 동일하게 맞춤
@Composable
private fun MiniWidgetPreview(color: Color) {
    val isDark = isSystemInDarkTheme()
    val baseBg = if (isDark) Color(0xFF0D1B2A) else Color.White
    val trackColor = lerp(baseBg, color, 0.12f)
    val fillColor = lerp(baseBg, color, 0.35f)
    val rate = 0.75f // 미리보기 예시 진행률
    Box(
        modifier = Modifier
            .size(width = 80.dp, height = 94.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(rate)
                .align(Alignment.BottomCenter)
                .background(fillColor)
        )
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_water_drop),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text("8", color = color, fontWeight = FontWeight.Bold, fontSize = 34.sp)
        }
    }
}

// 크기·모서리 반경은 실기기(Galaxy A25, One UI 2x2 셀) 실측값 기준 — 화면 전체 폭이 아니라
// 가로보다 세로가 긴 카드. cornerRadius 20.dp는 RectangularWidget.kt와 동일한 값을 그대로 사용
//
// 2026-07-11 리디자인: 얇은 바 + "워터링" 라벨 대신, 리터럴 컵 채움 그래픽 + "오늘 총 OOOml"
// 표시로 바뀐 RectangularWidget.kt의 실제 렌더링과 동일하게 맞춤
@Composable
private fun CardWidgetPreview(color: Color) {
    val isDark = isSystemInDarkTheme()
    val surfaceColor = if (isDark) Color(0xFF0D1B2A) else Color.White
    val onSurface = if (isDark) Color.White else Color(0xFF0D1B2A)
    val onSurfaceSecondary = onSurface.copy(alpha = 0.6f)
    val rate = 0.75f // 미리보기 예시 진행률 (6/8잔)
    Column(
        modifier = Modifier
            .width(190.dp)
            .height(220.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(surfaceColor)
            .padding(18.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_water_drop),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.widget_total_ml_today, 1500), color = color, fontSize = 15.sp)
        }
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CupGraphic(
                rate = rate,
                color = color,
                modifier = Modifier.size(width = 64.dp, height = 96.dp)
            )
            Spacer(Modifier.width(18.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("6", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = onSurface)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.glasses_with_slash, 8), fontSize = 16.sp, color = onSurfaceSecondary)
                }
                Spacer(Modifier.height(4.dp))
                Text("75%", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            stringResource(R.string.widget_motivation_almost_there),
            fontSize = 14.sp,
            color = onSurfaceSecondary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

// RectangularWidget.kt(CupBitmap.kt)와 동일한 좌표계(70x104, 위가 넓고 아래가 좁은 유리잔 형태)로
// 그리는 미리보기용 컵 그래픽 — 위젯 쪽은 Glance 제약으로 android.graphics.Bitmap에 미리 그리지만,
// 여기는 일반 Compose라 Canvas + clipPath로 바로 그림
@Composable
private fun CupGraphic(rate: Float, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sx = size.width / 70f
        val sy = size.height / 104f
        val path = Path().apply {
            moveTo(9f * sx, 4f * sy)
            lineTo(61f * sx, 4f * sy)
            lineTo(54f * sx, 98f * sy)
            quadraticTo(54f * sx, 102f * sy, 50f * sx, 102f * sy)
            lineTo(20f * sx, 102f * sy)
            quadraticTo(16f * sx, 102f * sy, 16f * sx, 98f * sy)
            close()
        }
        val fillTop = 4f * sy + (1f - rate) * (100f - 4f) * sy
        clipPath(path) {
            drawRect(
                color = color.copy(alpha = 0.85f),
                topLeft = Offset(0f, fillTop),
                size = Size(size.width, size.height - fillTop)
            )
        }
        drawPath(path = path, color = color, style = Stroke(width = 3f * sx))
    }
}

// 크기·모서리 반경은 실기기(Galaxy A25, One UI 2x1 셀) 실측값 기준 — 가로:세로 비율 약 2:1.
// cornerRadius 16.dp는 NarrowWidget.kt와 동일한 값을 그대로 사용
//
// 2026-07-11 리디자인: 얇은 바 대신 배경 자체가 진행률만큼 좌→우로 채워지는 NarrowWidget.kt의
// 실제 렌더링과 동일하게 맞춤(폭이 부족할 때 퍼센트를 숨기는 반응형 로직은 고정 크기 미리보기라
// 재현하지 않고, 항상 다 보이는 상태로 표시)
@Composable
private fun WideWidgetPreview(color: Color) {
    val isDark = isSystemInDarkTheme()
    val baseBg = if (isDark) Color(0xFF0D1B2A) else Color.White
    val trackColor = lerp(baseBg, color, 0.12f)
    val fillColor = lerp(baseBg, color, 0.35f)
    val onSurface = if (isDark) Color.White else Color(0xFF0D1B2A)
    val rate = 0.75f // 미리보기 예시 진행률
    Box(
        modifier = Modifier
            .width(170.dp)
            .height(86.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(rate)
                .background(fillColor)
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_water_drop),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text("6", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = onSurface)
            Spacer(Modifier.weight(1f))
            Text("75%", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = color)
        }
    }
}
