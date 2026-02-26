package com.habitao.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.habitao.core.datastore.AppSettings
import com.habitao.core.datastore.AppSettingsManager
import com.habitao.core.ui.theme.HabitaoTheme
import com.habitao.feature.habits.ui.CreateHabitScreen
import com.habitao.feature.habits.ui.HabitsScreen
import com.habitao.feature.habits.ui.StatsScreen
import com.habitao.feature.pomodoro.ui.FullScreenClockScreen
import com.habitao.feature.pomodoro.ui.PomodoroScreen
import com.habitao.feature.routines.ui.CreateRoutineScreen
import com.habitao.feature.routines.ui.RoutinesScreen
import com.habitao.feature.settings.ui.SettingsScreen
import com.habitao.feature.settings.ui.SettingsTabOption
import com.habitao.feature.tasks.ui.CreateTaskScreen
import com.habitao.feature.tasks.ui.TasksScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

// -- Type-safe route definitions --

@Serializable
object HabitsRoute

@Serializable
object PomodoroRoute

@Serializable
object StatsRoute

@Serializable
object RoutinesRoute

@Serializable
object TasksRoute

@Serializable
object SettingsRoute

@Serializable
object FullScreenClockRoute

@Serializable
data class CreateRoutineRoute(val routineId: String? = null)

@Serializable
data class CreateTaskRoute(val taskId: String? = null)

@Serializable
object CreateHabitRoute

@Serializable
data class EditHabitRoute(val habitId: String)

/**
 * Bottom navigation tab definition tied to route types.
 */
private enum class Tab(
    val id: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: Any,
) {
    STATS(
        id = "stats",
        label = "Stats",
        selectedIcon = Icons.Filled.BarChart,
        unselectedIcon = Icons.Outlined.BarChart,
        route = StatsRoute,
    ),
    HABITS(
        id = "habits",
        label = "Habits",
        selectedIcon = Icons.Filled.CheckCircle,
        unselectedIcon = Icons.Outlined.CheckCircleOutline,
        route = HabitsRoute,
    ),
    TASKS(
        id = "tasks",
        label = "Tasks",
        selectedIcon = Icons.Filled.TaskAlt,
        unselectedIcon = Icons.Outlined.TaskAlt,
        route = TasksRoute,
    ),
    ROUTINES(
        id = "routines",
        label = "Routines",
        selectedIcon = Icons.AutoMirrored.Filled.ListAlt,
        unselectedIcon = Icons.AutoMirrored.Outlined.ListAlt,
        route = RoutinesRoute,
    ),
    POMODORO(
        id = "pomodoro",
        label = "Pomodoro",
        selectedIcon = Icons.Filled.Timer,
        unselectedIcon = Icons.Outlined.Timer,
        route = PomodoroRoute,
    ),
    ;

    companion object {
        fun fromId(id: String): Tab? = entries.firstOrNull { tab -> tab.id == id }
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val appSettingsManager by lazy { AppSettingsManager(applicationContext) }

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
            HabitaoApp(appSettingsManager = appSettingsManager)
        }
    }
}

@Composable
private fun HabitaoApp(appSettingsManager: AppSettingsManager) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val coroutineScope = rememberCoroutineScope()
    var showMoreSheet by rememberSaveable { mutableStateOf(false) }

    val appSettings by
        remember(appSettingsManager) {
            appSettingsManager.settings.map<AppSettings, AppSettings?> { settings -> settings }
        }.collectAsStateWithLifecycle(initialValue = null)

    val settings = appSettings
    if (settings == null) {
        Box(modifier = Modifier.fillMaxSize())
        return
    }

    HabitaoTheme(themeMode = settings.themeMode) {

    val selectedTabs = remember(settings.bottomNavTabs, settings.maxVisibleTabs) {
        resolveSelectedTabs(settings.bottomNavTabs, settings.maxVisibleTabs)
    }
    val hiddenTabs = remember(selectedTabs) {
        Tab.entries.filterNot(selectedTabs::contains)
    }
    val defaultLaunchTab = remember(settings.defaultLaunchTab) {
        resolveDefaultLaunchTab(settings.defaultLaunchTab)
    }
    val allTabOptions = remember {
        Tab.entries.map { tab ->
            SettingsTabOption(
                id = tab.id,
                label = tab.label,
            )
        }
    }

    var startDestinationTabId by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(defaultLaunchTab.id) {
        if (startDestinationTabId == null) {
            startDestinationTabId = defaultLaunchTab.id
        }
    }

    val startDestinationTab = Tab.fromId(startDestinationTabId ?: Tab.HABITS.id) ?: Tab.HABITS

    val showBottomBar =
        currentDestination?.let { destination ->
            Tab.entries.any { tab -> destination.hasRoute(tab.route::class) }
        } ?: true

    val navigateToTab: (Tab) -> Unit = { tab ->
        navController.navigate(tab.route) {
            // Pop up to the start destination to avoid building up a large stack.
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    if (showMoreSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showMoreSheet = false },
            sheetState = sheetState,
        ) {
            MoreMenuSheet(
                hiddenTabs = hiddenTabs,
                onHiddenTabSelected = { hiddenTab ->
                    showMoreSheet = false
                    navigateToTab(hiddenTab)
                },
                onSettingsSelected = {
                    showMoreSheet = false
                    navController.navigate(SettingsRoute)
                },
            )
        }
    }

        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    HabitaoNavigationBar(
                        currentDestination = currentDestination,
                        visibleTabs = selectedTabs,
                        hiddenTabs = hiddenTabs,
                        onTabSelected = navigateToTab,
                        onMoreSelected = { showMoreSheet = true },
                    )
                }
            },
        ) { paddingValues ->
            val bottomPad = Modifier.padding(bottom = paddingValues.calculateBottomPadding())

            NavHost(
                navController = navController,
                startDestination = startDestinationTab.route,
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
                    PomodoroScreen(
                        onOpenFullScreen = { navController.navigate(FullScreenClockRoute) },
                    )
                }
            }

            composable<StatsRoute> {
                Box(modifier = bottomPad) {
                    StatsScreen()
                }
            }

            composable<RoutinesRoute> {
                Box(modifier = bottomPad) {
                    RoutinesScreen(
                        onAddRoutine = { navController.navigate(CreateRoutineRoute()) },
                        onEditRoutine = { routineId ->
                            navController.navigate(CreateRoutineRoute(routineId = routineId))
                        },
                    )
                }
            }

            composable<TasksRoute> {
                Box(modifier = bottomPad) {
                    TasksScreen(
                        onAddTask = { navController.navigate(CreateTaskRoute()) },
                        onEditTask = { taskId ->
                            navController.navigate(CreateTaskRoute(taskId = taskId))
                        },
                    )
                }
            }

                composable<SettingsRoute> {
                    SettingsScreen(
                        selectedBottomTabIds = selectedTabs.map(Tab::id),
                        allTabs = allTabOptions,
                        defaultLaunchTabId = defaultLaunchTab.id,
                        maxVisibleTabs = settings.maxVisibleTabs,
                        themeMode = settings.themeMode,
                        onBottomTabsChanged = { tabIds ->
                            val normalizedTabIds = resolveSelectedTabs(tabIds, settings.maxVisibleTabs).map(Tab::id)
                            coroutineScope.launch {
                                appSettingsManager.setBottomNavTabs(normalizedTabIds)
                            }
                        },
                        onDefaultLaunchTabChanged = { tabId ->
                            val normalizedTabId = resolveDefaultLaunchTab(tabId).id
                            coroutineScope.launch {
                                appSettingsManager.setDefaultLaunchTab(normalizedTabId)
                            }
                        },
                        onMaxVisibleTabsChanged = { count ->
                            coroutineScope.launch {
                                appSettingsManager.setMaxVisibleTabs(count)
                            }
                        },
                        onThemeModeChanged = { themeMode ->
                            coroutineScope.launch {
                                appSettingsManager.setThemeMode(themeMode)
                            }
                        },
                        onNavigateBack = { navController.popBackStack() },
                    )
                }

            // -- Full-screen destinations (no bottom bar) --
            composable<CreateRoutineRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<CreateRoutineRoute>()
                CreateRoutineScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onRoutineCreated = { navController.popBackStack() },
                    routineId = route.routineId,
                )
            }

            composable<CreateTaskRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<CreateTaskRoute>()
                CreateTaskScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onTaskCreated = { navController.popBackStack() },
                    taskId = route.taskId,
                )
            }

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

            composable<FullScreenClockRoute> {
                FullScreenClockScreen(
                    onClose = { navController.popBackStack() },
                )
            }
            }
        }
    }
}

@Composable
private fun HabitaoNavigationBar(
    currentDestination: NavDestination?,
    visibleTabs: List<Tab>,
    hiddenTabs: List<Tab>,
    onTabSelected: (Tab) -> Unit,
    onMoreSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        visibleTabs.forEach { tab ->
            val isSelected =
                currentDestination?.hierarchy?.any { destination ->
                    destination.hasRoute(tab.route::class)
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

        val isMoreSelected =
            currentDestination?.hierarchy?.any { destination ->
                hiddenTabs.any { hiddenTab -> destination.hasRoute(hiddenTab.route::class) }
            } == true

        NavigationBarItem(
            selected = isMoreSelected,
            onClick = onMoreSelected,
            icon = {
                Icon(
                    imageVector = Icons.Default.MoreHoriz,
                    contentDescription = "More",
                )
            },
            label = { Text("More") },
        )
    }
}

@Composable
private fun MoreMenuSheet(
    hiddenTabs: List<Tab>,
    onHiddenTabSelected: (Tab) -> Unit,
    onSettingsSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        hiddenTabs.forEach { hiddenTab ->
            ListItem(
                headlineContent = { Text(hiddenTab.label) },
                leadingContent = {
                    Icon(
                        imageVector = hiddenTab.unselectedIcon,
                        contentDescription = hiddenTab.label,
                    )
                },
                modifier = Modifier.clickable { onHiddenTabSelected(hiddenTab) },
            )
        }

        HorizontalDivider()

        ListItem(
            headlineContent = { Text("Settings") },
            leadingContent = {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                )
            },
            modifier = Modifier.clickable(onClick = onSettingsSelected),
        )
    }
}

private fun resolveSelectedTabs(savedTabIds: List<String>, maxTabs: Int = 4): List<Tab> {
    val preferredTabs = savedTabIds.mapNotNull(Tab::fromId).distinct()
    val remainingTabs = Tab.entries.filterNot(preferredTabs::contains)
    return (preferredTabs + remainingTabs).take(maxTabs)
}

private fun resolveDefaultLaunchTab(savedTabId: String): Tab {
    return Tab.fromId(savedTabId) ?: Tab.HABITS
}
