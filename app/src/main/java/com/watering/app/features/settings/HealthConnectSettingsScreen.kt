package com.watering.app.features.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.watering.app.R
import com.watering.app.core.service.HealthConnectService
import com.watering.app.ui.theme.AppBackgroundGradient
import com.watering.app.ui.theme.AppInfoBannerBackgroundColor
import com.watering.app.ui.theme.AppInfoBannerBorderColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthConnectSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val hydrationSyncUiState by viewModel.hydrationSyncUiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

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

    HydrationSyncDialogs(
        uiState = hydrationSyncUiState,
        onDismiss = viewModel::dismissHydrationSyncState
    )

    Box(modifier = Modifier.fillMaxSize().background(AppBackgroundGradient)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.settings_section_health)) },
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
                SettingSwitchRow(
                    title = stringResource(R.string.settings_hydration_sync_title),
                    subtitle = stringResource(R.string.settings_hydration_sync_subtitle),
                    checked = settings.healthConnectEnabled,
                    onCheckedChange = viewModel::onHydrationSyncToggle
                )

                if (settings.healthConnectEnabled) {
                    HealthConnectVisibilityHint(
                        onOpenSettings = {
                            val intent = Intent("android.health.connect.action.HEALTH_HOME_SETTINGS")
                            if (intent.resolveActivity(context.packageManager) != null) {
                                context.startActivity(intent)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HealthConnectVisibilityHint(onOpenSettings: () -> Unit) {
    val infoColor = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(AppInfoBannerBackgroundColor, RoundedCornerShape(12.dp))
            .border(1.5.dp, AppInfoBannerBorderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onOpenSettings)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Info, contentDescription = null, tint = infoColor)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(R.string.settings_hydration_sync_other_app_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                stringResource(R.string.settings_hydration_sync_other_app_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = infoColor)
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
                        android.net.Uri.parse("market://details?id=com.google.android.apps.healthdata")
                    )
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    }
                    onDismiss()
                }) {
                    Text(stringResource(R.string.settings_health_connect_open_play_store))
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
                    Text(stringResource(R.string.settings_health_connect_dialog_ok))
                }
            }
        )
    }
}
