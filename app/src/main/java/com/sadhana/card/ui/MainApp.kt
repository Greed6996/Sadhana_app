package com.sadhana.card.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sadhana.card.data.preferences.ThemeMode
import com.sadhana.card.ui.daily.DailyEntryScreen
import com.sadhana.card.ui.daily.DailyEntryViewModel
import com.sadhana.card.ui.legal.TermsConsentScreen
import com.sadhana.card.ui.legal.TermsPrivacyViewerScreen
import com.sadhana.card.ui.navigation.SadhanaBottomBar
import com.sadhana.card.ui.navigation.Screen
import com.sadhana.card.ui.settings.SettingsScreen
import com.sadhana.card.ui.splash.SplashScreen
import com.sadhana.card.ui.weekly.WeeklyMatrixScreen
import com.sadhana.card.ui.weekly.WeeklyMatrixViewModel
import kotlinx.coroutines.launch

object AppRoutes {
    const val TERMS_CONSENT = "terms_consent"
    const val MAIN_SCAFFOLD = "main_scaffold"
    const val SETTINGS = "settings"
    const val TERMS_VIEWER = "terms_viewer"
}

@Composable
fun MainApp(
    dailyViewModel: DailyEntryViewModel = hiltViewModel(),
    weeklyViewModel: WeeklyMatrixViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val isTermsAccepted by mainViewModel.isTermsAccepted.collectAsStateWithLifecycle()
    val themeMode by mainViewModel.themeMode.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    if (isTermsAccepted == null) {
        // Read BEFORE the first screen renders: show lightweight splash (no flash of terms)
        SplashScreen()
    } else {
        val rootNavController = rememberNavController()
        val startDestination = if (isTermsAccepted == true) {
            AppRoutes.MAIN_SCAFFOLD
        } else {
            AppRoutes.TERMS_CONSENT
        }

        NavHost(
            navController = rootNavController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(AppRoutes.TERMS_CONSENT) {
                TermsConsentScreen(
                    onAcceptTerms = {
                        // Await the DataStore write before navigating
                        mainViewModel.acceptTerms()
                        // Remove Terms screen from back stack (popUpTo inclusive) so Back never returns to it
                        rootNavController.navigate(AppRoutes.MAIN_SCAFFOLD) {
                            popUpTo(AppRoutes.TERMS_CONSENT) {
                                inclusive = true
                            }
                        }
                    }
                )
            }

            composable(AppRoutes.MAIN_SCAFFOLD) {
                MainTabsScaffold(
                    dailyViewModel = dailyViewModel,
                    weeklyViewModel = weeklyViewModel,
                    onOpenSettings = {
                        rootNavController.navigate(AppRoutes.SETTINGS)
                    }
                )
            }

            composable(AppRoutes.SETTINGS) {
                SettingsScreen(
                    currentThemeMode = themeMode,
                    onSelectThemeMode = { newMode ->
                        coroutineScope.launch {
                            mainViewModel.setThemeMode(newMode)
                        }
                    },
                    onNavigateToTerms = {
                        rootNavController.navigate(AppRoutes.TERMS_VIEWER)
                    },
                    onBack = {
                        rootNavController.popBackStack()
                    }
                )
            }

            composable(AppRoutes.TERMS_VIEWER) {
                TermsPrivacyViewerScreen(
                    onBack = {
                        rootNavController.popBackStack()
                    }
                )
            }
        }
    }
}

@Composable
private fun MainTabsScaffold(
    dailyViewModel: DailyEntryViewModel,
    weeklyViewModel: WeeklyMatrixViewModel,
    onOpenSettings: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Daily.route

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            SadhanaBottomBar(
                currentRoute = currentRoute,
                onNavigateToRoute = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Daily.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Daily.route) {
                DailyEntryScreen(
                    viewModel = dailyViewModel,
                    onOpenSettings = onOpenSettings
                )
            }

            composable(Screen.Weekly.route) {
                WeeklyMatrixScreen(
                    viewModel = weeklyViewModel,
                    onDayClick = { date ->
                        dailyViewModel.selectDate(date)
                        navController.navigate(Screen.Daily.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}
