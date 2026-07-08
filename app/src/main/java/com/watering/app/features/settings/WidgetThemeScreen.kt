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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.watering.app.ui.theme.AppCardBackgroundColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetThemeScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

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
            }
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                WidgetThemeSetting(
                    selectedTheme = settings.widgetTheme,
                    onSelectTheme = viewModel::updateWidgetTheme
                )
            }
        }
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
    selectedTheme: WidgetTheme,
    onSelectTheme: (WidgetTheme) -> Unit
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
                val isSelected = theme == selectedTheme
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
                        .clickable { onSelectTheme(theme) }
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
        WidgetPreviewCard(color = selectedTheme.accentColor, shape = previewShape)
    }
}

@Composable
private fun WidgetPreviewCard(color: Color, shape: WidgetPreviewShape) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = AppCardBackgroundColor,
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

@Composable
private fun MiniWidgetPreview(color: Color) {
    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(RoundedCornerShape(48.dp))
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(R.drawable.ic_water_drop),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Text("8", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 26.sp)
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.4f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(36.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White)
                )
            }
        }
    }
}

@Composable
private fun CardWidgetPreview(color: Color) {
    val surfaceColor = if (isSystemInDarkTheme()) Color(0xFF0D1B2A) else Color.White
    val onSurface = if (isSystemInDarkTheme()) Color.White else Color(0xFF0D1B2A)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceColor)
            .padding(16.dp)
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
            Text("6", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = onSurface)
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.glasses_with_slash, 8), fontSize = 13.sp, color = onSurface.copy(alpha = 0.6f))
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
            fontSize = 12.sp,
            color = onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun WideWidgetPreview(color: Color) {
    val surfaceColor = if (isSystemInDarkTheme()) Color(0xFF0D1B2A) else Color.White
    val onSurface = if (isSystemInDarkTheme()) Color.White else Color(0xFF0D1B2A)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceColor)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_water_drop),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text("6", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = onSurface)
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.75f)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text("75%", fontSize = 13.sp, color = onSurface.copy(alpha = 0.6f))
    }
}
