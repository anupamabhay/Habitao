package com.habitao.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.habitao.core.ui.theme.HabitaoTheme
import com.habitao.feature.habits.ui.CreateHabitScreen
import com.habitao.feature.habits.ui.HabitsScreen
import com.habitao.feature.habits.ui.StatsScreen
import com.habitao.feature.pomodoro.ui.PomodoroScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.Serializable

// -- Type-safe route definitions --

@Serializable
object HabitsRoute

@Serializable
object PomodoroRoute

@Serializable
object StatsRoute

@Serializable
object SettingsRoute

@Serializable
object CreateHabitRoute

@Serializable
data class EditHabitRoute(val habitId: String)

/**
 * Bottom navigation tab definition tied to route types.
 */
private enum class Tab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: Any,
) {
    HABITS("Habits", Icons.Filled.CheckCircle, Icons.Outlined.CheckCircleOutline, HabitsRoute),
    POMODORO("Pomodoro", Icons.Filled.Timer, Icons.Outlined.Timer, PomodoroRoute),
    STATS("Stats", Icons.Filled.BarChart, Icons.Outlined.BarChart, StatsRoute),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings, SettingsRoute),
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle =
                SystemBarStyle.auto(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT,
                ),
            navigationBarStyle =
                SystemBarStyle.auto(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT,
                ),
        )
        setContent {
            HabitaoTheme {
                HabitaoApp()
            }
        }
    }
}

@Composable
private fun HabitaoApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Hide bottom bar on full-screen destinations (create/edit)
    val showBottomBar =
        currentDestination?.let { dest ->
            Tab.entries.any { tab -> dest.hasRoute(tab.route::class) }
        } ?: true

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                HabitaoNavigationBar(
                    currentDestination = currentDestination,
                    onTabSelected = { tab ->
                        navController.navigate(tab.route) {
                            // Pop up to the start destination to avoid building up a large stack
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { paddingValues ->
        val bottomPad = Modifier.padding(bottom = paddingValues.calculateBottomPadding())

        NavHost(
            navController = navController,
            startDestination = HabitsRoute,
        ) {
            // -- Tab destinations --
            composable<HabitsRoute> {
                Box(modifier = bottomPad) {
                    HabitsScreen(
                        onAddHabit = { navController.navigate(CreateHabitRoute) },
                        onEditHabit = { habitId ->
                            navController.navigate(EditHabitRoute(habitId))
                        },
                    )
                }
            }

            composable<PomodoroRoute> {
                Box(modifier = bottomPad) {
                    PomodoroScreen()
                }
            }

            composable<StatsRoute> {
                Box(modifier = bottomPad) {
                    StatsScreen()
                }
            }

            composable<SettingsRoute> {
                SettingsPlaceholder(
                    modifier = Modifier.padding(paddingValues),
                )
            }

            // -- Full-screen destinations (no bottom bar) --
            composable<CreateHabitRoute> {
                CreateHabitScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onHabitCreated = { navController.popBackStack() },
                )
            }

            composable<EditHabitRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<EditHabitRoute>()
                CreateHabitScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onHabitCreated = { navController.popBackStack() },
                    habitId = route.habitId,
                )
            }
        }
    }
}

@Composable
private fun HabitaoNavigationBar(
    currentDestination: androidx.navigation.NavDestination?,
    onTabSelected: (Tab) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        Tab.entries.forEach { tab ->
            val isSelected =
                currentDestination?.hierarchy?.any {
                    it.hasRoute(tab.route::class)
                } == true

            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector =
                            if (isSelected) {
                                tab.selectedIcon
                            } else {
                                tab.unselectedIcon
                            },
                        contentDescription = tab.label,
                    )
                },
                label = { Text(tab.label) },
            )
        }
    }
}

@Composable
private fun SettingsPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Settings coming soon",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
