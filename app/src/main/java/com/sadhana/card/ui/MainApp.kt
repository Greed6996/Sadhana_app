package com.sadhana.card.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sadhana.card.ui.daily.DailyEntryScreen
import com.sadhana.card.ui.daily.DailyEntryViewModel
import com.sadhana.card.ui.navigation.SadhanaBottomBar
import com.sadhana.card.ui.navigation.Screen
import com.sadhana.card.ui.theme.CreamBackground
import com.sadhana.card.ui.weekly.WeeklyMatrixScreen
import com.sadhana.card.ui.weekly.WeeklyMatrixViewModel

@Composable
fun MainApp(
    dailyViewModel: DailyEntryViewModel = hiltViewModel(),
    weeklyViewModel: WeeklyMatrixViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Daily.route

    Scaffold(
        containerColor = CreamBackground,
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
                    viewModel = dailyViewModel
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
