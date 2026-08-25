package com.gameturbo.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gameturbo.app.data.GameRepository
import com.gameturbo.app.data.SessionHistoryRepository
import com.gameturbo.app.monitor.ThermalInfo
import com.gameturbo.app.monitor.MemoryInfo
import com.gameturbo.app.profiles.PerformanceProfile
import com.gameturbo.app.ui.screens.*
import com.gameturbo.app.ui.theme.*

sealed class Screen(val route: String, val label: String, val icon: String) {
    data object Dashboard : Screen("dashboard", "Dashboard", "🏠")
    data object GameLibrary : Screen("games", "Games", "🎮")
    data object Profiles : Screen("profiles", "Profiles", "⚙️")
    data object History : Screen("history", "History", "📊")
}

@Composable
fun GameTurboNavigation(
    thermalInfo: ThermalInfo,
    memoryInfo: MemoryInfo,
    isGamingModeActive: Boolean,
    currentGame: String?,
    currentProfile: PerformanceProfile,
    gameRepository: GameRepository,
    sessionRepository: SessionHistoryRepository,
    onStartGamingMode: () -> Unit,
    onStopGamingMode: () -> Unit,
    onProfileSelected: (PerformanceProfile) -> Unit
) {
    val navController = rememberNavController()
    val screens = listOf(
        Screen.Dashboard,
        Screen.GameLibrary,
        Screen.Profiles,
        Screen.History
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        containerColor = DarkBackground,
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                contentColor = DarkText
            ) {
                screens.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        icon = {
                            Text(
                                text = screen.icon,
                                fontSize = if (selected) 22.sp else 18.sp
                            )
                        },
                        label = {
                            Text(
                                text = screen.label,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentBlue,
                            selectedTextColor = AccentBlue,
                            unselectedIconColor = DarkTextSecondary,
                            unselectedTextColor = DarkTextSecondary,
                            indicatorColor = AccentBlue.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    thermalInfo = thermalInfo,
                    memoryInfo = memoryInfo,
                    isGamingModeActive = isGamingModeActive,
                    currentGame = currentGame,
                    currentProfile = currentProfile,
                    onStartGamingMode = onStartGamingMode,
                    onStopGamingMode = onStopGamingMode
                )
            }
            composable(Screen.GameLibrary.route) {
                GameLibraryScreen(gameRepository = gameRepository)
            }
            composable(Screen.Profiles.route) {
                ProfilesScreen(
                    currentProfile = currentProfile,
                    onProfileSelected = onProfileSelected
                )
            }
            composable(Screen.History.route) {
                HistoryScreen(repository = sessionRepository)
            }
        }
    }
}
