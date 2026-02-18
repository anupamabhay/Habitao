package com.habitao.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.habitao.core.ui.theme.HabitaoTheme
import com.habitao.feature.habits.ui.CreateHabitScreen
import com.habitao.feature.habits.ui.HabitsScreen
import com.habitao.feature.habits.ui.StatsScreen
import dagger.hilt.android.AndroidEntryPoint

/**
 * Navigation destinations within the app.
 */
private sealed class Screen {
    data object HabitsList : Screen()

    data object CreateHabit : Screen()

    data class EditHabit(val habitId: String) : Screen()

    data object Stats : Screen()

    data object Settings : Screen()
}

/**
 * Bottom navigation tab definition.
 */
private enum class Tab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    HABITS("Habits", Icons.Filled.CheckCircle, Icons.Outlined.CheckCircleOutline),
    STATS("Stats", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
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
    var currentScreen by remember { mutableStateOf<Screen>(Screen.HabitsList) }
    var selectedTab by rememberSaveable { mutableStateOf(Tab.HABITS.ordinal) }

    // Full-screen destinations without bottom bar
    when (val screen = currentScreen) {
        Screen.CreateHabit -> {
            BackHandler { currentScreen = Screen.HabitsList }
            CreateHabitScreen(
                onNavigateBack = { currentScreen = Screen.HabitsList },
                onHabitCreated = { currentScreen = Screen.HabitsList },
            )
            return
        }
        is Screen.EditHabit -> {
            BackHandler { currentScreen = Screen.HabitsList }
            CreateHabitScreen(
                onNavigateBack = { currentScreen = Screen.HabitsList },
                onHabitCreated = { currentScreen = Screen.HabitsList },
                habitId = screen.habitId,
            )
            return
        }
        else -> { /* Tab destinations handled below */ }
    }

    // Tab destinations with bottom bar
    Scaffold(
        bottomBar = {
            HabitaoNavigationBar(
                selectedTab = Tab.entries[selectedTab],
                onTabSelected = { tab ->
                    selectedTab = tab.ordinal
                    currentScreen =
                        when (tab) {
                            Tab.HABITS -> Screen.HabitsList
                            Tab.STATS -> Screen.Stats
                            Tab.SETTINGS -> Screen.Settings
                        }
                },
            )
        },
    ) { paddingValues ->
        // Only apply bottom padding from the outer Scaffold.
        // Inner screens have their own Scaffolds that handle top bar / status bar insets.
        val bottomPad = Modifier.padding(bottom = paddingValues.calculateBottomPadding())

        when (Tab.entries[selectedTab]) {
            Tab.HABITS -> {
                Box(modifier = bottomPad) {
                    HabitsScreen(
                        onAddHabit = { currentScreen = Screen.CreateHabit },
                        onEditHabit = { habitId ->
                            currentScreen = Screen.EditHabit(habitId)
                        },
                    )
                }
            }
            Tab.STATS -> {
                Box(modifier = bottomPad) {
                    StatsScreen()
                }
            }
            Tab.SETTINGS -> {
                SettingsPlaceholder(
                    modifier = Modifier.padding(paddingValues),
                )
            }
        }
    }
}

@Composable
private fun HabitaoNavigationBar(
    selectedTab: Tab,
    onTabSelected: (Tab) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        Tab.entries.forEach { tab ->
            val isSelected = selectedTab == tab
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
