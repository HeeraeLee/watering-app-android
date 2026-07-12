package com.watering.app.features.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.watering.app.R
import com.watering.app.ui.theme.AppBackgroundGradient
import com.watering.app.ui.theme.GoalGlassesEquivalentRow
import com.watering.app.ui.theme.SettingsMenuTileBackgroundColor
import com.watering.app.ui.theme.SettingsMenuTileIconColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToWidgetTheme: () -> Unit,
    onNavigateToHealthConnect: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToAppInfo: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val weightGoalUiState by viewModel.weightGoalUiState.collectAsStateWithLifecycle()
    val weightGoalSubtitle by viewModel.weightGoalSubtitle.collectAsStateWithLifecycle()
    val csvExportUiState by viewModel.csvExportUiState.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showResetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(message = msg)
            viewModel.clearSnackbar()
        }
    }

    LaunchedEffect(csvExportUiState) {
        when (val state = csvExportUiState) {
            is CsvExportUiState.Success -> {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, state.uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, null))
                viewModel.dismissCsvExportState()
            }
            is CsvExportUiState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                viewModel.dismissCsvExportState()
            }
            else -> {}
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.settings_reset_confirm_title)) },
            text = { Text(stringResource(R.string.settings_reset_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetAllData()
                    showResetDialog = false
                }) {
                    Text(stringResource(R.string.settings_reset_confirm_button), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.settings_dialog_cancel))
                }
            }
        )
    }

    WeightGoalDialogs(
        uiState = weightGoalUiState,
        onWeightInputChange = viewModel::onWeightInputChange,
        onDismiss = viewModel::dismissWeightGoalState,
        onApply = viewModel::applyWeightGoal
    )

    Box(modifier = Modifier.fillMaxSize().background(AppBackgroundGradient)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.settings_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.content_description_back))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item { SectionHeader(stringResource(R.string.settings_section_recording)) }

                item {
                    DailyGoalSetting(
                        goal = settings.dailyGoal,
                        cupSize = settings.cupSize,
                        onGoalChange = viewModel::updateDailyGoal
                    )
                }

                item { Spacer(Modifier.height(8.dp)) }

                item {
                    CupSizeSetting(
                        goal = settings.dailyGoal,
                        cupSize = settings.cupSize,
                        onCupSizeChange = viewModel::updateCupSize
                    )
                }

                item { Spacer(Modifier.height(16.dp)) }

                item {
                    WeightGoalRow(subtitle = weightGoalSubtitle, onClick = viewModel::openWeightGoalDialog)
                }

                // 체중 목표/CSV 내보내기 사이는 "더보기" 섹션 메뉴 행들과 같은 패턴으로 —
                // 별도 Spacer 없이 각 Row 자체의 vertical padding(12dp)만으로 간격을 둠(2026-07-10)
                item {
                    CsvExportRow(onClick = viewModel::exportCsv)
                }

                item { SectionDivider() }
                item { SectionHeader(stringResource(R.string.settings_section_more)) }

                item {
                    SettingsMenuRow(
                        icon = Icons.Filled.Notifications,
                        title = stringResource(R.string.settings_section_notification),
                        subtitle = stringResource(R.string.settings_menu_notification_subtitle),
                        onClick = onNavigateToNotifications
                    )
                }
                item {
                    SettingsMenuRow(
                        icon = Icons.Filled.Favorite,
                        title = stringResource(R.string.settings_section_health),
                        subtitle = stringResource(R.string.settings_menu_health_subtitle),
                        onClick = onNavigateToHealthConnect
                    )
                }
                item {
                    SettingsMenuRow(
                        icon = Icons.Filled.Palette,
                        title = stringResource(R.string.settings_section_widget_theme),
                        subtitle = stringResource(R.string.settings_menu_widget_theme_subtitle),
                        onClick = onNavigateToWidgetTheme
                    )
                }
                item {
                    SettingsMenuRow(
                        icon = Icons.Filled.Backup,
                        title = stringResource(R.string.settings_section_backup),
                        subtitle = stringResource(R.string.settings_menu_backup_subtitle),
                        onClick = onNavigateToBackup
                    )
                }
                item {
                    SettingsMenuRow(
                        icon = Icons.Filled.Info,
                        title = stringResource(R.string.settings_app_info),
                        subtitle = null,
                        onClick = onNavigateToAppInfo
                    )
                }

                item { SectionDivider() }

                item {
                    TextButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            stringResource(R.string.settings_reset_button),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

// 라이트모드 차콜 그레이(#5A5A66)는 다크모드 배경(딥 틸 그라디언트)에서 대비 1.74:1까지 떨어져
// 거의 안 보이던 문제(2026-07-09) — 웹 목업 6안 비교 후 "골드"(연속 기록 카드 보더와 동일 계열)
// 채택, 대비 7.51:1
private val SettingsAccentColor: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFFF5C860) else Color(0xFF5A5A66)

@Composable
private fun SettingsMenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MenuIconTile(icon)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// 설정 화면 아이콘 행 공용 타일 (더보기 메뉴 행 / 체중 목표 / CSV 내보내기) — Fable UI 리뷰 findings에서
// 체중 목표는 타일, CSV 내보내기는 맨 글리프로 처리 방식이 갈려있다는 지적으로 웹 목업 비교 후
// "전부 타일로 통일" 안 채택(2026-07-08)
@Composable
private fun MenuIconTile(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(SettingsMenuTileBackgroundColor, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = SettingsMenuTileIconColor, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = SettingsAccentColor,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
}

// ml이 주역, 잔 수는 CupSizeSetting 아래 보조 텍스트로 표시 — 온보딩 목표 설정 화면과
// 동일한 패턴으로 통일(2026-07-10). goal(잔 수)이 여전히 저장 단위라 +/- 한 번의 증감폭은
// 선택된 컵 크기와 같음
@Composable
private fun DailyGoalSetting(goal: Int, cupSize: Int, onGoalChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(R.string.label_daily_goal), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { onGoalChange(goal - 1) },
                enabled = goal > 1,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.content_description_goal_decrease), tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                text = stringResource(R.string.onboarding_goal_ml, goal * cupSize),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.widthIn(min = 110.dp)
            )
            IconButton(
                onClick = { onGoalChange(goal + 1) },
                enabled = goal < 20,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.content_description_goal_increase), tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

// 실제 유명 텀블러/카페/생활 컵 용량에 앵커링한 프리셋 — 임의의 라운드 숫자 대신 사용자가
// "우리 집 컵/자주 쓰는 텀블러랑 똑같네"라고 알아볼 수 있게 함(웹 목업 8안 비교 후 F안 채택:
// 카테고리 그룹화). 출처: 종이컵 6.5oz(190ml), 일반 유리 물컵 200ml 안팎, 스타벅스 톨/그란데/
// 벤티 12·16·20oz(355/473/591ml), 메가커피 아이스 24oz(710ml), 생수 500ml, 스탠리 퀜처 30oz(887ml)
private data class CupSizePreset(val ml: Int, @androidx.annotation.StringRes val nameRes: Int)

private val DailyCupPresets = listOf(
    CupSizePreset(190, R.string.cup_size_paper_cup),
    CupSizePreset(200, R.string.cup_size_regular_glass)
)
private val CafeCupPresets = listOf(
    CupSizePreset(355, R.string.cup_size_starbucks_tall),
    CupSizePreset(473, R.string.cup_size_starbucks_grande),
    CupSizePreset(591, R.string.cup_size_starbucks_venti),
    CupSizePreset(710, R.string.cup_size_megacoffee)
)
private val TumblerBottlePresets = listOf(
    CupSizePreset(500, R.string.cup_size_water_bottle),
    CupSizePreset(887, R.string.cup_size_stanley_quencher)
)

@Composable
private fun CupSizeSetting(goal: Int, cupSize: Int, onCupSizeChange: (Int) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(stringResource(R.string.label_cup_size), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        CupSizeGroup(stringResource(R.string.cup_size_group_daily), DailyCupPresets, cupSize, onCupSizeChange)
        CupSizeGroup(stringResource(R.string.cup_size_group_cafe), CafeCupPresets, cupSize, onCupSizeChange)
        CupSizeGroup(stringResource(R.string.cup_size_group_tumbler), TumblerBottlePresets, cupSize, onCupSizeChange)
        Spacer(Modifier.height(8.dp))
        GoalGlassesEquivalentRow(glasses = goal)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CupSizeGroup(
    title: String,
    presets: List<CupSizePreset>,
    cupSize: Int,
    onCupSizeChange: (Int) -> Unit
) {
    Text(
        title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        presets.forEach { preset ->
            FilterChip(
                selected = cupSize == preset.ml,
                onClick = { onCupSizeChange(preset.ml) },
                label = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${preset.ml}ml", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(preset.nameRes), style = MaterialTheme.typography.labelSmall)
                    }
                },
                colors = selectedChipColors()
            )
        }
    }
}

// 선택된 칩 배경(연보라)+흰 텍스트 대비 부족 (Fable UI 리뷰 findings — 알림 설정, 2026-07-08)
// 컵 크기(설정 화면)/알림 간격(알림 설정 화면)이 이 함수를 공유해서 흰 텍스트를 쓰는데,
// 배경이 파스텔이라 대비가 약함(White on #B8A8E8 ≈2.1:1). 웹 목업 비교 후 "배경색 자체를
// 더 진하게" 안 채택 — 텍스트는 흰색 유지, 배경만 어둡게 해 두 화면 모두 자동 반영됨
@Composable
internal fun selectedChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = Color(0xFF9B87D9),
    selectedLabelColor = Color.White
)

@Composable
private fun WeightGoalRow(subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MenuIconTile(Icons.Filled.MonitorWeight)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.settings_weight_goal_title), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CsvExportRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MenuIconTile(Icons.Filled.FileDownload)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.settings_csv_export_title), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(
                stringResource(R.string.settings_csv_export_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeightGoalDialogs(
    uiState: WeightGoalUiState,
    onWeightInputChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onApply: () -> Unit
) {
    if (uiState !is WeightGoalUiState.Editing) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_weight_goal_dialog_title)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.settings_weight_goal_explanation),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = uiState.weightInput,
                    onValueChange = onWeightInputChange,
                    label = { Text(stringResource(R.string.settings_weight_goal_input_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (uiState.recommendedCups != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.settings_weight_goal_recommended, uiState.recommendedCups),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onApply, enabled = uiState.recommendedCups != null) {
                Text(stringResource(R.string.settings_weight_goal_confirm_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_dialog_cancel))
            }
        }
    )
}

@Composable
internal fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
