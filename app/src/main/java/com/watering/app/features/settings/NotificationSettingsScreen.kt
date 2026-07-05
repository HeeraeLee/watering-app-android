package com.watering.app.features.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.watering.app.R
import com.watering.app.ui.theme.AppBackgroundGradient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val notificationPermissionGranted by viewModel.notificationPermissionGranted.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 시스템 알림 설정 화면을 다녀온 뒤에도 최신 권한 상태를 반영
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshNotificationPermission()
    }

    Box(modifier = Modifier.fillMaxSize().background(AppBackgroundGradient)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.settings_section_notification)) },
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
                if (settings.notificationEnabled && !notificationPermissionGranted) {
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

                SettingSwitchRow(
                    title = stringResource(R.string.settings_notification_switch_title),
                    subtitle = stringResource(R.string.settings_notification_switch_subtitle),
                    checked = settings.notificationEnabled,
                    onCheckedChange = viewModel::updateNotificationEnabled
                )

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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IntervalSetting(interval: Int, onIntervalChange: (Int) -> Unit) {
    val intervals = listOf(
        30 to stringResource(R.string.settings_interval_30min),
        60 to stringResource(R.string.settings_interval_1hour),
        90 to stringResource(R.string.settings_interval_90min),
        120 to stringResource(R.string.settings_interval_2hour),
        150 to stringResource(R.string.settings_interval_150min),
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
                    label = { Text(label) },
                    colors = selectedChipColors()
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
