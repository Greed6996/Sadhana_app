package com.sadhana.card.ui

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sadhana.card.data.auth.AuthState
import com.sadhana.card.data.preferences.ThemeMode
import com.sadhana.card.data.update.UpdateState
import com.sadhana.card.ui.auth.AuthChoiceDialog
import com.sadhana.card.ui.auth.CounselorPendingDialog
import com.sadhana.card.ui.auth.CounselorPendingScreen
import com.sadhana.card.ui.auth.HistoricalSyncConsentDialog
import com.sadhana.card.ui.components.ProfileDialog
import com.sadhana.card.ui.counselor.CounselorDashboardViewModel
import com.sadhana.card.ui.counselor.CounselorDirectoryScreen
import com.sadhana.card.ui.daily.DailyEntryScreen
import com.sadhana.card.ui.daily.DailyEntryViewModel
import com.sadhana.card.ui.legal.TermsConsentScreen
import com.sadhana.card.ui.legal.TermsPrivacyViewerScreen
import com.sadhana.card.ui.navigation.SadhanaBottomBar
import com.sadhana.card.ui.navigation.Screen
import com.sadhana.card.ui.settings.SettingsScreen
import com.sadhana.card.ui.splash.SplashScreen
import com.sadhana.card.ui.update.UpdateDialog
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
    val updateState by mainViewModel.updateState.collectAsStateWithLifecycle()
    val sadhakaName by mainViewModel.sadhakaName.collectAsStateWithLifecycle()
    val counselorName by mainViewModel.counselorName.collectAsStateWithLifecycle()
    val authState by mainViewModel.authState.collectAsStateWithLifecycle()
    val isAuthLoading by mainViewModel.isAuthLoading.collectAsStateWithLifecycle()
    val isCheckingStatus by mainViewModel.isCheckingStatus.collectAsStateWithLifecycle()
    val authErrorMessage by mainViewModel.authErrorMessage.collectAsStateWithLifecycle()
    val showHistoricalSyncPrompt by mainViewModel.showHistoricalSyncPrompt.collectAsStateWithLifecycle()
    val localRecordCount by mainViewModel.localRecordCount.collectAsStateWithLifecycle()
    val showCounselorPendingPrompt by mainViewModel.showCounselorPendingPrompt.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isOnboardingProfileDismissed by rememberSaveable { mutableStateOf(false) }
    var isOnboardingAuthDismissed by rememberSaveable { mutableStateOf(false) }
    var showManualAuthChoiceDialog by rememberSaveable { mutableStateOf(false) }

    // Automatically dismiss auth modal dialogs upon successful authentication
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated || authState is AuthState.PendingApproval) {
            showManualAuthChoiceDialog = false
            isOnboardingAuthDismissed = true
        }
    }

    LaunchedEffect(authErrorMessage) {
        val err = authErrorMessage
        if (!err.isNullOrBlank()) {
            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
            mainViewModel.clearAuthError()
        }
    }

    UpdateDialog(
        updateState = updateState,
        onDismiss = mainViewModel::dismissUpdate,
        onUpdateNow = mainViewModel::downloadUpdate,
        onInstallNow = mainViewModel::installUpdate,
        onResetState = mainViewModel::resetUpdateState
    )

    // Pop-up 2: Profile Dialog (Immediately after accepting Terms if sadhakaName is blank)
    val shouldShowOnboardingProfile = isTermsAccepted == true &&
        sadhakaName.isBlank() &&
        !isOnboardingProfileDismissed

    if (shouldShowOnboardingProfile) {
        ProfileDialog(
            initialSadhakaName = sadhakaName,
            initialCounselorName = counselorName,
            isOnboarding = true,
            onDismiss = {
                isOnboardingProfileDismissed = true
            },
            onSave = { newSadhaka, newCounselor ->
                mainViewModel.saveProfile(newSadhaka, newCounselor)
                isOnboardingProfileDismissed = true
            }
        )
    }

    // Pop-up 3: AuthChoiceDialog (Devotee or Counselor login or Continue Offline)
    val shouldShowOnboardingAuth = isTermsAccepted == true &&
        (sadhakaName.isNotBlank() || isOnboardingProfileDismissed) &&
        authState is AuthState.Unauthenticated &&
        !isOnboardingAuthDismissed

    if (shouldShowOnboardingAuth || showManualAuthChoiceDialog) {
        AuthChoiceDialog(
            isLoading = isAuthLoading,
            onSelectRole = { role ->
                mainViewModel.launchGoogleSignIn(context, role)
                showManualAuthChoiceDialog = false
            },
            onContinueOffline = {
                isOnboardingAuthDismissed = true
                showManualAuthChoiceDialog = false
            }
        )
    }

    // Counselor Pending Approval Dialog (shown when user selects counselor role during dialog)
    if (showCounselorPendingPrompt && authState !is AuthState.PendingApproval) {
        CounselorPendingDialog(
            onDismiss = mainViewModel::dismissCounselorPendingPrompt
        )
    }

    // Historical Sync Consent Dialog
    if (showHistoricalSyncPrompt) {
        HistoricalSyncConsentDialog(
            localRecordCount = localRecordCount,
            onConfirmSyncAll = mainViewModel::claimHistoricalRecords,
            onSkipHistorical = mainViewModel::skipHistoricalRecords
        )
    }

    if (isTermsAccepted == null) {
        // Read BEFORE the first screen renders: show lightweight splash (no flash of terms)
        SplashScreen()
    } else if (authState is AuthState.PendingApproval) {
        // Dedicated non-dismissible Pending Approval screen for counselors with live snapshot sync
        val pendingUser = authState as AuthState.PendingApproval
        CounselorPendingScreen(
            email = pendingUser.email,
            displayName = pendingUser.displayName,
            isCheckingStatus = isCheckingStatus,
            onCheckStatus = mainViewModel::refreshAuthStatus,
            onSignOut = mainViewModel::signOut
        )
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
                    authState = authState,
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
                    },
                    onCheckForUpdates = {
                        mainViewModel.checkForUpdates(isManualCheck = true)
                    },
                    isCheckingForUpdates = updateState is UpdateState.Checking,
                    sadhakaName = sadhakaName,
                    counselorName = counselorName,
                    onSaveProfile = { newSadhaka, newCounselor ->
                        mainViewModel.saveProfile(newSadhaka, newCounselor)
                    },
                    authState = authState,
                    onSignInClick = {
                        showManualAuthChoiceDialog = true
                    },
                    onSignOutClick = mainViewModel::signOut
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
    authState: AuthState,
    onOpenSettings: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Daily.route

    val isCounselor = (authState as? AuthState.Authenticated)?.let {
        it.role.equals("counselor", ignoreCase = true) && it.isCounselorApproved
    } ?: false

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            SadhanaBottomBar(
                currentRoute = currentRoute,
                isCounselor = isCounselor,
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

            if (isCounselor) {
                composable(Screen.Counselor.route) {
                    val counselorViewModel: CounselorDashboardViewModel = hiltViewModel()
                    CounselorDirectoryScreen(viewModel = counselorViewModel)
                }
            }
        }
    }
}
