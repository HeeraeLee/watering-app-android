package com.watering.app.features.settings

import androidx.annotation.StringRes
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.watering.app.R
import com.watering.app.core.model.WidgetTheme
import com.watering.app.ui.theme.AppBackgroundGradient
import com.watering.app.ui.theme.WidgetPreviewCardBackgroundColor

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

@Composable
private fun WidgetPreviewCard(color: Color, shape: WidgetPreviewShape) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = WidgetPreviewCardBackgroundColor,
        modifier = Modifier.fillMaxWidth()
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
// 목표 미달성(평상시) 상태를 그대로 미리보여줌 — 배경은 테마색이 아니라 흰색/다크 배경이고
// 아이콘·숫자·진행바가 테마색을 띠는 CircularWidget.kt의 실제 렌더링과 동일하게 맞춤.
// (예전엔 목표 달성 상태처럼 배경 전체가 테마색 + 흰 아이콘으로 고정되어 있어 실제 위젯과
// 색이 다르게 보이는 버그가 있었음, 2026-07-10)
@Composable
private fun MiniWidgetPreview(color: Color) {
    val bgColor = if (isSystemInDarkTheme()) Color(0xFF0D1B2A) else Color.White
    val barTrackColor = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.3f) else color.copy(alpha = 0.15f)
    Box(
        modifier = Modifier
            .size(width = 80.dp, height = 94.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(R.drawable.ic_water_drop),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Text("8", color = color, fontWeight = FontWeight.Bold, fontSize = 28.sp)
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(barTrackColor)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(36.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color)
                )
            }
        }
    }
}

// 크기·모서리 반경은 실기기(Galaxy A25, One UI 2x2 셀) 실측값 기준 — 화면 전체 폭이 아니라
// 가로보다 세로가 긴 카드. cornerRadius 20.dp는 RectangularWidget.kt와 동일한 값을 그대로 사용
@Composable
private fun CardWidgetPreview(color: Color) {
    val surfaceColor = if (isSystemInDarkTheme()) Color(0xFF0D1B2A) else Color.White
    val onSurface = if (isSystemInDarkTheme()) Color.White else Color(0xFF0D1B2A)
    Column(
        modifier = Modifier
            .width(190.dp)
            .height(220.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(surfaceColor)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_water_drop),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.widget_app_label), color = color, fontSize = 13.sp)
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("6", fontSize = 38.sp, fontWeight = FontWeight.Bold, color = onSurface)
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.glasses_with_slash, 8), fontSize = 14.sp, color = onSurface.copy(alpha = 0.6f))
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.75f)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.widget_motivation_almost_there),
            fontSize = 11.5.sp,
            color = onSurface.copy(alpha = 0.6f)
        )
    }
}

// 크기·모서리 반경은 실기기(Galaxy A25, One UI 2x1 셀) 실측값 기준 — 가로:세로 비율 약 2:1.
// cornerRadius 16.dp는 NarrowWidget.kt와 동일한 값을 그대로 사용
@Composable
private fun WideWidgetPreview(color: Color) {
    val surfaceColor = if (isSystemInDarkTheme()) Color(0xFF0D1B2A) else Color.White
    val onSurface = if (isSystemInDarkTheme()) Color.White else Color(0xFF0D1B2A)
    Row(
        modifier = Modifier
            .width(170.dp)
            .height(86.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceColor)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_water_drop),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text("6", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = onSurface)
        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.75f)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
        Spacer(Modifier.width(6.dp))
        Text("75%", fontSize = 12.sp, color = onSurface.copy(alpha = 0.6f))
    }
}
