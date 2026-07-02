package com.watering.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.watering.app.features.home.HomeScreen
import com.watering.app.features.home.HomeViewModel
import com.watering.app.features.onboarding.OnboardingScreen
import com.watering.app.features.onboarding.OnboardingViewModel
import com.watering.app.features.settings.SettingsScreen
import com.watering.app.features.stats.StatsScreen
import com.watering.app.features.premium.PremiumScreen

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Stats : Screen("stats")
    object Settings : Screen("settings")
    object Premium : Screen("premium")
}

@Composable
fun WateringNavGraph(
    quickRecord: Boolean = false,
    navController: NavHostController = rememberNavController()
) {
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val isOnboardingDone by onboardingViewModel.isOnboardingDone.collectAsStateWithLifecycle()

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
                quickRecord = quickRecord,
                onNavigateToStats = { navController.navigate(Screen.Stats.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToPremium = { navController.navigate(Screen.Premium.route) }
            )
        }
        composable(Screen.Stats.route) {
            StatsScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToPremium = { navController.navigate(Screen.Premium.route) }
            )
        }
        composable(Screen.Premium.route) {
            PremiumScreen(onBack = { navController.popBackStack() })
        }
    }
}
