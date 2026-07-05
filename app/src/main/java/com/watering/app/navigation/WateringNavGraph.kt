package com.watering.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.watering.app.core.service.AnalyticsService
import com.watering.app.features.home.HomeScreen
import com.watering.app.features.home.HomeViewModel
import com.watering.app.features.onboarding.OnboardingScreen
import com.watering.app.features.onboarding.OnboardingViewModel
import com.watering.app.features.settings.AppInfoScreen
import com.watering.app.features.settings.BackupScreen
import com.watering.app.features.settings.HealthConnectSettingsScreen
import com.watering.app.features.settings.NotificationSettingsScreen
import com.watering.app.features.settings.SettingsScreen
import com.watering.app.features.settings.WidgetThemeScreen
import com.watering.app.features.stats.SmartStatsScreen
import com.watering.app.features.stats.StatsScreen
import com.watering.app.features.premium.PremiumScreen

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Stats : Screen("stats")
    object SmartStats : Screen("smart_stats")
    object Settings : Screen("settings")
    object Premium : Screen("premium")
    object SettingsWidgetTheme : Screen("settings_widget_theme")
    object SettingsHealthConnect : Screen("settings_health_connect")
    object SettingsNotifications : Screen("settings_notifications")
    object SettingsBackup : Screen("settings_backup")
    object SettingsAppInfo : Screen("settings_app_info")
}

@Composable
fun WateringNavGraph(
    analyticsService: AnalyticsService,
    navController: NavHostController = rememberNavController()
) {
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val isOnboardingDone by onboardingViewModel.isOnboardingDone.collectAsStateWithLifecycle()

    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { entry ->
            entry.destination.route?.let { analyticsService.logScreenView(it) }
        }
    }

    // DataStore 로드 전(null): 빈 화면 유지 — 보통 100ms 이내 해소
    if (isOnboardingDone == null) return

    NavHost(
        navController = navController,
        startDestination = if (isOnboardingDone == true) Screen.Home.route else Screen.Onboarding.route
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Home.route) {
            val viewModel: HomeViewModel = hiltViewModel()
            HomeScreen(
                viewModel = viewModel,
                onNavigateToStats = { navController.navigate(Screen.Stats.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.Stats.route) {
            StatsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToSmartStats = { navController.navigate(Screen.SmartStats.route) }
            )
        }
        composable(Screen.SmartStats.route) {
            SmartStatsScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToWidgetTheme = { navController.navigate(Screen.SettingsWidgetTheme.route) },
                onNavigateToHealthConnect = { navController.navigate(Screen.SettingsHealthConnect.route) },
                onNavigateToNotifications = { navController.navigate(Screen.SettingsNotifications.route) },
                onNavigateToBackup = { navController.navigate(Screen.SettingsBackup.route) },
                onNavigateToAppInfo = { navController.navigate(Screen.SettingsAppInfo.route) }
            )
        }
        composable(Screen.SettingsWidgetTheme.route) {
            WidgetThemeScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.SettingsHealthConnect.route) {
            HealthConnectSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.SettingsNotifications.route) {
            NotificationSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.SettingsBackup.route) {
            BackupScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.SettingsAppInfo.route) {
            AppInfoScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Premium.route) {
            PremiumScreen(onBack = { navController.popBackStack() })
        }
    }
}
