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
import androidx.compose.material3.Surface
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
import com.watering.app.ui.theme.AppCardBackgroundColor
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
                    // TODO: A안(RecordingSettingsCard) vs C안(RecordingSettingsList) 비교 중 — 지금은 C안 표시
                    RecordingSettingsList(
                        goal = settings.dailyGoal,
                        cupSize = settings.cupSize,
                        onGoalChange = viewModel::updateDailyGoal,
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

// "기록 설정"(하루 목표+컵 크기) 가독성 개선 웹 목업 4안 중 A안(올인원 카드) 채택 시도 —
// 두 설정을 하나의 연보라 톤 카드로 묶고 구분선으로 나눔. C안(구분선 리스트)과 실기기에서
// 비교해보고 owner가 최종 선택 예정
// 다크모드 미대응 발견(2026-07-13, owner 제보) — 라이트 전용 하드코딩 색이라 다크모드에서도
// 안 바뀌던 문제. 다른 카드들(AppCardBackgroundColor 등)과 동일한 다크 톤(딥 틸)으로 통일
private val RecordingCardBackgroundColor: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF1C4D49) else Color(0xFFFAF8FF)

// 카드 내부 구분선(하루 목표/컵 크기 사이) — 기본 MaterialTheme 구분선(회색)은 다크 틸 카드
// 위에서 톤이 안 맞고 붕 떠 보인다는 owner 피드백(2026-07-13)으로 카드 톤에 맞춘 전용 색 지정.
// 라이트는 기존 연보라 유지, 다크는 저투명도 화이트로 카드 배경에 자연스럽게 스며들게 함
private val RecordingCardDividerColor: Color
    @Composable get() = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.14f) else Color(0xFFE8E1F5)

// "하루 목표"/"컵 크기" 라벨, 그룹 제목(일상/카페/텀블러), 선택 안 된 칩 텍스트 톤 — 웹 목업
// 3안(소프트 차콜/웜 그레이/라벤더 그레이) 비교 후 "웜 그레이" 채택(2026-07-16). 기존 onSurface
// (#1C1B1F)가 카드 배경(연보라)과 대비가 너무 강하고 "컵 크기"만 Bold라 유독 도드라져 보인다는
// 피드백 — 라이트는 옅은 회색으로, 다크는 기존 onSurface 그대로 유지(다크 카드 배경에선 이미
// 문제 없던 값이라 이번 변경 범위 밖)
private val RecordingLabelColor: Color
    @Composable get() = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onSurface else Color(0xFF5A5A66)

@Composable
private fun RecordingSettingsCard(
    goal: Int,
    cupSize: Int,
    onGoalChange: (Int) -> Unit,
    onCupSizeChange: (Int) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = RecordingCardBackgroundColor,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            DailyGoalSetting(goal = goal, cupSize = cupSize, onGoalChange = onGoalChange)
            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = RecordingCardDividerColor)
            CupSizeSetting(goal = goal, cupSize = cupSize, onCupSizeChange = onCupSizeChange)
        }
    }
}

// C안(구분선 리스트)에서는 이 카드+그림자 없이 Row 내용만 재사용해야 해서 내용을 분리
@Composable
private fun DailyGoalRow(goal: Int, cupSize: Int, onGoalChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(R.string.label_daily_goal), style = MaterialTheme.typography.titleMedium, color = RecordingLabelColor)
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

// ml이 주역, 잔 수는 CupSizeSetting 아래 보조 텍스트로 표시 — 온보딩 목표 설정 화면과
// 동일한 패턴으로 통일(2026-07-10). goal(잔 수)이 여전히 저장 단위라 +/- 한 번의 증감폭은
// 선택된 컵 크기와 같음
@Composable
private fun DailyGoalSetting(goal: Int, cupSize: Int, onGoalChange: (Int) -> Unit) {
    // 다른 설정 항목들과 똑같은 평범한 Row라 눈에 안 띈다는 owner 피드백으로, 홈 화면
    // StreakCard와 동일한 카드 스타일(흰 배경+그림자)만 적용해 시각적으로 튀게 함
    // (웹 목업 6안 중 B안의 카드+그림자만 채택, 아이콘 뱃지는 제외)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = AppCardBackgroundColor,
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        DailyGoalRow(goal = goal, cupSize = cupSize, onGoalChange = onGoalChange)
    }
}

// "기록 설정" 가독성 개선 웹 목업 4안 중 C안(구분선 기반 리스트, iOS 설정 앱 그룹 테이블 느낌) —
// 하나의 흰 카드 안에 하루 목표/컵 크기를 얇은 구분선으로만 나눔(각 행 자체엔 그림자 없음).
// A안(RecordingSettingsCard)과 실기기에서 비교 후 owner가 최종 선택 예정
@Composable
private fun RecordingSettingsList(
    goal: Int,
    cupSize: Int,
    onGoalChange: (Int) -> Unit,
    onCupSizeChange: (Int) -> Unit
) {
    // C안(구분선 리스트) 구조 + A안(연보라 톤) 배경색 조합 — owner 요청으로 두 안을 섞음
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = RecordingCardBackgroundColor,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column {
            DailyGoalRow(goal = goal, cupSize = cupSize, onGoalChange = onGoalChange)
            HorizontalDivider(color = RecordingCardDividerColor)
            Box(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp)) {
                CupSizeSetting(goal = goal, cupSize = cupSize, onCupSizeChange = onCupSizeChange)
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

// 줄간격 웹 목업 6안 중 C안(넉넉하게) 채택 — 타이틀↔첫 그룹, 그룹↔그룹 간격 모두 16dp로 통일
private val CupSizeGroupGap = 16.dp
private val CupSizeGroupTitleToChipsGap = 4.dp

@Composable
private fun CupSizeSetting(goal: Int, cupSize: Int, onCupSizeChange: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.label_cup_size),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = RecordingLabelColor
        )
        Spacer(Modifier.height(CupSizeGroupGap))
        CupSizeGroup(stringResource(R.string.cup_size_group_daily), DailyCupPresets, cupSize, onCupSizeChange)
        Spacer(Modifier.height(CupSizeGroupGap))
        CupSizeGroup(stringResource(R.string.cup_size_group_cafe), CafeCupPresets, cupSize, onCupSizeChange)
        Spacer(Modifier.height(CupSizeGroupGap))
        CupSizeGroup(stringResource(R.string.cup_size_group_tumbler), TumblerBottlePresets, cupSize, onCupSizeChange)
        Spacer(Modifier.height(14.dp))
        GoalGlassesEquivalentRow(glasses = goal)
    }
}

// 그룹 라벨 좌측 컬러 바 — 칩 선택색(selectedChipColors)과 동일한 연보라로 통일
private val CupSizeGroupBarColor = Color(0xFF9B87D9)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CupSizeGroup(
    title: String,
    presets: List<CupSizePreset>,
    cupSize: Int,
    onCupSizeChange: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = CupSizeGroupTitleToChipsGap)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(14.dp)
                .background(CupSizeGroupBarColor)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = RecordingLabelColor
        )
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        presets.forEach { preset ->
            val selected = cupSize == preset.ml
            FilterChip(
                selected = selected,
                onClick = { onCupSizeChange(preset.ml) },
                label = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${preset.ml}ml",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selected) Color.White else RecordingLabelColor
                        )
                        Text(
                            stringResource(preset.nameRes),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) Color.White else RecordingLabelColor
                        )
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
