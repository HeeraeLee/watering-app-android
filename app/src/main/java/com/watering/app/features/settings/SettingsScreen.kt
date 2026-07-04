package com.watering.app.features.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.auth.FirebaseUser
import com.watering.app.R
import com.watering.app.core.model.WidgetTheme
import com.watering.app.core.service.HealthConnectService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ACTION_SENDTO + EXTRA_SUBJECT는 한국어·특수문자를 시스템이 알아서 인코딩하므로 별도 URL 인코딩 불필요
private fun Context.sendSupportEmail(subject: String) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(getString(R.string.support_email)))
        putExtra(Intent.EXTRA_SUBJECT, subject)
    }
    if (intent.resolveActivity(packageManager) != null) {
        startActivity(intent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToPremium: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
    backupViewModel: BackupViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val notificationPermissionGranted by viewModel.notificationPermissionGranted.collectAsStateWithLifecycle()
    val currentUser by backupViewModel.currentUser.collectAsStateWithLifecycle()
    val backupUiState by backupViewModel.backupUiState.collectAsStateWithLifecycle()
    val weightGoalUiState by viewModel.weightGoalUiState.collectAsStateWithLifecycle()
    val hydrationSyncUiState by viewModel.hydrationSyncUiState.collectAsStateWithLifecycle()
    val csvExportUiState by viewModel.csvExportUiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val privacyPolicyUrl = stringResource(R.string.privacy_policy_url)
    var showResetDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }

    val weightPermissionLauncher = rememberLauncherForActivityResult(
        contract = viewModel.healthConnectPermissionContract()
    ) { granted ->
        viewModel.onWeightPermissionResult(granted)
    }

    LaunchedEffect(weightGoalUiState) {
        if (weightGoalUiState is WeightGoalUiState.NeedsPermission) {
            weightPermissionLauncher.launch(HealthConnectService.WEIGHT_PERMISSIONS)
        }
    }

    val hydrationPermissionLauncher = rememberLauncherForActivityResult(
        contract = viewModel.healthConnectPermissionContract()
    ) { granted ->
        viewModel.onHydrationPermissionResult(granted)
    }

    LaunchedEffect(hydrationSyncUiState) {
        if (hydrationSyncUiState is HydrationSyncUiState.NeedsPermission) {
            hydrationPermissionLauncher.launch(HealthConnectService.HYDRATION_PERMISSIONS)
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

    // 시스템 알림 설정 화면을 다녀온 뒤에도 최신 권한 상태를 반영
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshNotificationPermission()
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text(stringResource(R.string.settings_restore_confirm_title)) },
            text = { Text(stringResource(R.string.settings_restore_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    backupViewModel.restore()
                    showRestoreDialog = false
                }) {
                    Text(stringResource(R.string.settings_restore_confirm_button), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text(stringResource(R.string.settings_dialog_cancel))
                }
            }
        )
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
        onDismiss = viewModel::dismissWeightGoalState,
        onApply = viewModel::applyRecommendedGoal
    )

    HydrationSyncDialogs(
        uiState = hydrationSyncUiState,
        onDismiss = viewModel::dismissHydrationSyncState
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.content_description_back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                PremiumSection(
                    isPremium = settings.isPremium,
                    onClick = onNavigateToPremium
                )
            }
            item { SectionDivider() }

            item { SectionHeader(stringResource(R.string.settings_section_widget_theme)) }
            item {
                WidgetThemeSetting(
                    isPremium = settings.isPremium,
                    selectedTheme = settings.widgetTheme,
                    onSelectTheme = viewModel::updateWidgetTheme,
                    onLockedClick = onNavigateToPremium
                )
            }
            item { SectionDivider() }

            item { SectionHeader(stringResource(R.string.settings_section_recording)) }

            item {
                DailyGoalSetting(
                    goal = settings.dailyGoal,
                    onGoalChange = viewModel::updateDailyGoal
                )
            }

            item { Spacer(Modifier.height(8.dp)) }

            item {
                CupSizeSetting(
                    cupSize = settings.cupSize,
                    onCupSizeChange = viewModel::updateCupSize
                )
            }

            item { Spacer(Modifier.height(8.dp)) }

            item {
                WeightGoalRow(
                    isPremium = settings.isPremium,
                    onClick = viewModel::requestWeightBasedGoal,
                    onLockedClick = onNavigateToPremium
                )
            }

            item { Spacer(Modifier.height(8.dp)) }

            item {
                CsvExportRow(
                    isPremium = settings.isPremium,
                    onClick = viewModel::exportCsv,
                    onLockedClick = onNavigateToPremium
                )
            }

            item { SectionDivider() }
            item { SectionHeader(stringResource(R.string.settings_section_health)) }

            item {
                SettingSwitchRow(
                    title = stringResource(R.string.settings_hydration_sync_title),
                    subtitle = stringResource(R.string.settings_hydration_sync_subtitle),
                    checked = settings.healthConnectEnabled,
                    onCheckedChange = viewModel::onHydrationSyncToggle
                )
            }

            item { SectionDivider() }
            item { SectionHeader(stringResource(R.string.settings_section_notification)) }

            if (settings.notificationEnabled && !notificationPermissionGranted) {
                item {
                    NotificationPermissionBanner(
                        onOpenSettings = {
                            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            } else {
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    .setData(Uri.fromParts("package", context.packageName, null))
                            }
                            context.startActivity(intent)
                        }
                    )
                }
            }

            item {
                SettingSwitchRow(
                    title = stringResource(R.string.settings_notification_switch_title),
                    subtitle = stringResource(R.string.settings_notification_switch_subtitle),
                    checked = settings.notificationEnabled,
                    onCheckedChange = viewModel::updateNotificationEnabled
                )
            }

            item {
                AnimatedVisibility(visible = settings.notificationEnabled) {
                    Column {
                        IntervalSetting(
                            interval = settings.notificationInterval,
                            onIntervalChange = viewModel::updateNotificationInterval
                        )
                        TimeAdjusterRow(
                            label = stringResource(R.string.settings_notification_start),
                            hour = settings.notificationStart,
                            onHourChange = { h ->
                                if (h < settings.notificationEnd) viewModel.updateNotificationStart(h)
                            },
                            range = 0..21
                        )
                        TimeAdjusterRow(
                            label = stringResource(R.string.settings_notification_end),
                            hour = settings.notificationEnd,
                            onHourChange = { h ->
                                if (h > settings.notificationStart) viewModel.updateNotificationEnd(h)
                            },
                            range = 1..23
                        )
                    }
                }
            }

            item { SectionDivider() }
            item { SectionHeader(stringResource(R.string.settings_section_backup)) }

            item {
                BackupSection(
                    currentUser = currentUser,
                    backupUiState = backupUiState,
                    onSignIn = { backupViewModel.signIn(context) },
                    onSignOut = backupViewModel::signOut,
                    onBackup = backupViewModel::backup,
                    onRestoreClick = { showRestoreDialog = true }
                )
            }

            item { SectionDivider() }
            item { SectionHeader(stringResource(R.string.settings_app_info)) }

            item {
                TextButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(privacyPolicyUrl))
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        stringResource(R.string.settings_privacy_policy),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            item {
                val feedbackSubject = stringResource(R.string.settings_feedback_email_subject)
                TextButton(
                    onClick = { context.sendSupportEmail(subject = feedbackSubject) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        stringResource(R.string.settings_send_feedback),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
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

            // 디버그 빌드 전용 — 실제 결제 없이 프리미엄 기능 테스트용 토글 (release 빌드에는 노출 안 됨)
            if (com.watering.app.BuildConfig.DEBUG) {
                item {
                    TextButton(
                        onClick = { viewModel.toggleDebugPremium() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            if (settings.isPremium) "[DEBUG] 프리미엄 끄기" else "[DEBUG] 프리미엄 켜기",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumSection(isPremium: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .let { if (isPremium) it else it.clickable(onClick = onClick) }
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.WorkspacePremium,
            contentDescription = null,
            tint = Color(0xFFFFC107)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (isPremium) stringResource(R.string.settings_premium_active) else stringResource(R.string.settings_premium_upgrade),
                style = MaterialTheme.typography.bodyLarge
            )
            if (!isPremium) {
                Text(
                    stringResource(R.string.feature_streak_protection_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WidgetThemeSetting(
    isPremium: Boolean,
    selectedTheme: WidgetTheme,
    onSelectTheme: (WidgetTheme) -> Unit,
    onLockedClick: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WidgetTheme.entries.forEach { theme ->
                val isSelected = isPremium && theme == selectedTheme
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(theme.accentColor.copy(alpha = if (isPremium) 1f else 0.35f))
                        .border(
                            width = if (isSelected) 3.dp else 0.dp,
                            color = MaterialTheme.colorScheme.onSurface,
                            shape = CircleShape
                        )
                        .clickable {
                            if (isPremium) onSelectTheme(theme) else onLockedClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (!isPremium) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = stringResource(theme.displayNameRes),
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        if (!isPremium) {
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.settings_widget_theme_locked_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NotificationPermissionBanner(onOpenSettings: () -> Unit) {
    val warningColor = Color(0xFFF57C00)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(warningColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.NotificationsOff, contentDescription = null, tint = warningColor)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(R.string.settings_notification_permission_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                stringResource(R.string.settings_notification_permission_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = onOpenSettings) {
            Text(stringResource(R.string.settings_notification_permission_open), color = warningColor)
        }
    }
}

@Composable
private fun BackupSection(
    currentUser: FirebaseUser?,
    backupUiState: BackupUiState,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onBackup: () -> Unit,
    onRestoreClick: () -> Unit
) {
    val infoColor = MaterialTheme.colorScheme.primary
    val isLoading = backupUiState is BackupUiState.Loading

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(infoColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (currentUser != null) Icons.Filled.CloudDone else Icons.Filled.Backup,
                contentDescription = null,
                tint = infoColor
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (currentUser != null) {
                    Text(
                        currentUser.email ?: stringResource(R.string.settings_backup_signed_in_default),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = when (backupUiState) {
                            is BackupUiState.Success -> stringResource(
                                R.string.settings_backup_last_backup,
                                formatBackupTimestamp(backupUiState.timestampMillis)
                            )
                            else -> stringResource(R.string.settings_backup_none)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        stringResource(R.string.settings_backup_signed_out_body),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        stringResource(R.string.settings_backup_signed_out_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (currentUser != null) {
                TextButton(onClick = onSignOut) { Text(stringResource(R.string.settings_backup_sign_out)) }
            } else {
                TextButton(onClick = onSignIn) { Text(stringResource(R.string.settings_backup_sign_in)) }
            }
        }

        if (currentUser != null) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onBackup,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.settings_backup_now))
                }
                OutlinedButton(
                    onClick = onRestoreClick,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.settings_backup_restore))
                }
            }
        }

        if (isLoading) {
            Spacer(Modifier.height(8.dp))
            CircularProgressIndicator(modifier = Modifier.size(20.dp))
        }
        if (backupUiState is BackupUiState.Error) {
            Spacer(Modifier.height(4.dp))
            Text(
                backupUiState.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

private fun formatBackupTimestamp(millis: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA).format(Date(millis))

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
private fun DailyGoalSetting(goal: Int, onGoalChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(R.string.label_daily_goal), style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { onGoalChange(goal - 1) },
                enabled = goal > 1,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.content_description_goal_decrease))
            }
            Text(
                text = stringResource(R.string.glasses_count, goal),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.widthIn(min = 80.dp)
            )
            IconButton(
                onClick = { onGoalChange(goal + 1) },
                enabled = goal < 20,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.content_description_goal_increase))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CupSizeSetting(cupSize: Int, onCupSizeChange: (Int) -> Unit) {
    val cupSizes = listOf(150, 200, 250, 300, 350, 500)

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(stringResource(R.string.label_cup_size), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            cupSizes.forEach { size ->
                FilterChip(
                    selected = cupSize == size,
                    onClick = { onCupSizeChange(size) },
                    label = { Text("${size}ml") }
                )
            }
        }
    }
}

@Composable
private fun WeightGoalRow(isPremium: Boolean, onClick: () -> Unit, onLockedClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (isPremium) onClick() else onLockedClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.MonitorWeight, contentDescription = null)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.settings_weight_goal_title), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(
                    if (isPremium) R.string.settings_weight_goal_subtitle else R.string.settings_weight_goal_locked_hint
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (!isPremium) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun CsvExportRow(isPremium: Boolean, onClick: () -> Unit, onLockedClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (isPremium) onClick() else onLockedClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.FileDownload, contentDescription = null)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.settings_csv_export_title), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(
                    if (isPremium) R.string.settings_csv_export_subtitle else R.string.settings_csv_export_locked_hint
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (!isPremium) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun WeightGoalDialogs(
    uiState: WeightGoalUiState,
    onDismiss: () -> Unit,
    onApply: (Int) -> Unit
) {
    val context = LocalContext.current

    when (uiState) {
        is WeightGoalUiState.Idle, is WeightGoalUiState.Loading, is WeightGoalUiState.NeedsPermission -> Unit

        is WeightGoalUiState.NotAvailable -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.settings_weight_goal_not_available_title)) },
            text = { Text(stringResource(R.string.settings_weight_goal_not_available_body)) },
            confirmButton = {
                TextButton(onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=com.google.android.apps.healthdata")
                    )
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    }
                    onDismiss()
                }) {
                    Text(stringResource(R.string.settings_weight_goal_open_play_store))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.settings_dialog_cancel))
                }
            }
        )

        is WeightGoalUiState.NoWeightData -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.settings_weight_goal_no_data_title)) },
            text = { Text(stringResource(R.string.settings_weight_goal_no_data_body)) },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.settings_weight_goal_dialog_ok))
                }
            }
        )

        is WeightGoalUiState.PermissionDenied -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.settings_weight_goal_permission_denied_title)) },
            text = { Text(stringResource(R.string.settings_weight_goal_permission_denied_body)) },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.settings_weight_goal_dialog_ok))
                }
            }
        )

        is WeightGoalUiState.Recommended -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.settings_weight_goal_confirm_title)) },
            text = {
                Text(
                    stringResource(R.string.settings_weight_goal_confirm_body, uiState.weightKg, uiState.cups)
                )
            },
            confirmButton = {
                TextButton(onClick = { onApply(uiState.cups) }) {
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
}

@Composable
private fun HydrationSyncDialogs(
    uiState: HydrationSyncUiState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    when (uiState) {
        is HydrationSyncUiState.Idle, is HydrationSyncUiState.NeedsPermission -> Unit

        is HydrationSyncUiState.NotAvailable -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.settings_hydration_sync_not_available_title)) },
            text = { Text(stringResource(R.string.settings_hydration_sync_not_available_body)) },
            confirmButton = {
                TextButton(onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=com.google.android.apps.healthdata")
                    )
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    }
                    onDismiss()
                }) {
                    Text(stringResource(R.string.settings_weight_goal_open_play_store))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.settings_dialog_cancel))
                }
            }
        )

        is HydrationSyncUiState.PermissionDenied -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.settings_hydration_sync_permission_denied_title)) },
            text = { Text(stringResource(R.string.settings_hydration_sync_permission_denied_body)) },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.settings_weight_goal_dialog_ok))
                }
            }
        )
    }
}

@Composable
private fun SettingSwitchRow(
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
            Text(title, style = MaterialTheme.typography.bodyLarge)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IntervalSetting(interval: Int, onIntervalChange: (Int) -> Unit) {
    val intervals = listOf(
        30 to stringResource(R.string.settings_interval_30min),
        60 to stringResource(R.string.settings_interval_1hour),
        120 to stringResource(R.string.settings_interval_2hour),
        180 to stringResource(R.string.settings_interval_3hour),
        240 to stringResource(R.string.settings_interval_4hour)
    )

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(stringResource(R.string.settings_interval_label), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            intervals.forEach { (minutes, label) ->
                FilterChip(
                    selected = interval == minutes,
                    onClick = { onIntervalChange(minutes) },
                    label = { Text(label) }
                )
            }
        }
    }
}

@Composable
private fun TimeAdjusterRow(
    label: String,
    hour: Int,
    onHourChange: (Int) -> Unit,
    range: IntRange
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { onHourChange(hour - 1) },
                enabled = hour > range.first
            ) {
                Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.settings_hour_decrease_content_description))
            }
            Text(
                text = "%02d:00".format(hour),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(56.dp)
            )
            IconButton(
                onClick = { onHourChange(hour + 1) },
                enabled = hour < range.last
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.settings_hour_increase_content_description))
            }
        }
    }
}
