package com.watering.app.features.home

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.watering.app.R
import com.watering.app.core.model.DrinkType
import com.watering.app.core.model.WaterEntry
import com.watering.app.features.record.RecordSheet
import com.watering.app.ui.theme.AppBackgroundGradient
import com.watering.app.ui.theme.AppCardBackgroundColor

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToStats: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val pendingAchievement by viewModel.pendingAchievement.collectAsStateWithLifecycle()
    val activity = LocalContext.current.findActivity()
    val snackbarHostState = remember { SnackbarHostState() }
    var showRecordSheet by remember { mutableStateOf(false) }

    val undoActionLabel = stringResource(R.string.home_snackbar_undo_action)
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            val result = snackbarHostState.showSnackbar(message = msg, actionLabel = undoActionLabel)
            if (result == SnackbarResult.ActionPerformed) viewModel.undoLastEntry()
            viewModel.clearSnackbar()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(AppBackgroundGradient)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(R.string.home_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    actions = {
                        IconButton(onClick = onNavigateToStats) {
                            Icon(Icons.Default.BarChart, contentDescription = stringResource(R.string.stats_title))
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_title))
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
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 48.dp)
        ) {
            item {
                Spacer(Modifier.height(16.dp))
                AchievementRing(
                    current = uiState.record.totalCount,
                    goal = uiState.record.goal,
                    rate = uiState.record.achievementRate,
                    isAchieved = uiState.record.isAchieved
                )
                Spacer(Modifier.height(28.dp))
            }

            item {
                Button(
                    onClick = { viewModel.addWater() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Text(stringResource(R.string.home_drink_water_button), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Filled.WaterDrop, contentDescription = null, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showRecordSheet = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(stringResource(R.string.home_select_other_drink))
                }
                Spacer(Modifier.height(20.dp))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StreakCard(
                        icon = Icons.Filled.LocalFireDepartment,
                        tint = MaterialTheme.colorScheme.tertiary,
                        label = stringResource(R.string.label_current_streak),
                        value = stringResource(R.string.streak_days_value, uiState.streak.currentStreak),
                        modifier = Modifier.weight(1f)
                    )
                    StreakCard(
                        icon = Icons.Filled.EmojiEvents,
                        tint = MaterialTheme.colorScheme.secondary,
                        label = stringResource(R.string.label_longest_streak),
                        value = stringResource(R.string.streak_days_value, uiState.streak.longestStreak),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(24.dp))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.home_today_record),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (uiState.record.entries.isNotEmpty()) {
                        Text(
                            stringResource(R.string.home_undo_last),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .clickable(onClick = { viewModel.undoLastEntry() })
                                .semantics { role = Role.Button }
                                .padding(12.dp)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            if (uiState.record.entries.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.WaterDrop,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                stringResource(R.string.home_empty_record),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(uiState.record.entries.reversed()) { entry ->
                    WaterEntryRow(entry = entry)
                    Spacer(Modifier.height(6.dp))
                }
            }
        }

        if (showRecordSheet) {
            RecordSheet(
                onDismiss = { showRecordSheet = false },
                onRecord = { amount, drinkType -> viewModel.addWaterCustom(amount, drinkType) }
            )
        }

        pendingAchievement?.let { achievement ->
            AchievementDialog(
                achievement = achievement,
                onDismiss = { viewModel.dismissAchievement(activity) }
            )
        }
        }
    }
}

private val AchievedColor = Color(0xFFBFA8DE)

@Composable
private fun AchievementRing(current: Int, goal: Int, rate: Double, isAchieved: Boolean) {
    val ringColor = if (isAchieved) AchievedColor else MaterialTheme.colorScheme.primary
    val animatedRate by animateFloatAsState(
        targetValue = rate.coerceIn(0.0, 1.0).toFloat(),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "ring"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 18.dp.toPx()
            drawArc(
                color = ringColor.copy(alpha = 0.15f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(strokeWidth)
            )
            if (animatedRate > 0f) {
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = animatedRate * 360f,
                    useCenter = false,
                    style = Stroke(strokeWidth, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$current",
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = ringColor
            )
            Text(
                text = stringResource(R.string.glasses_with_slash, goal),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isAchieved) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.label_goal_achieved_banner),
                    fontSize = 20.sp,
                    color = AchievedColor,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Text(
                    text = "${(rate * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StreakCard(
    icon: ImageVector,
    tint: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = AppCardBackgroundColor,
        shadowElevation = 3.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WaterEntryRow(entry: WaterEntry) {
    val emoji = entry.drinkType.emoji
    val drinkName = stringResource(entry.drinkType.displayNameRes)
    val time = java.time.Instant.ofEpochMilli(entry.timestampMillis)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalTime()
        .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = AppCardBackgroundColor,
        shadowElevation = 1.5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (entry.drinkType == DrinkType.WATER) {
                    Icon(
                        Icons.Filled.WaterDrop,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Text(emoji, fontSize = 22.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        drinkName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "${entry.amount}ml",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
