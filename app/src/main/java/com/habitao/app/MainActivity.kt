package com.habitao.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.habitao.core.ui.theme.HabitaoTheme
import com.habitao.feature.habits.ui.CreateHabitScreen
import com.habitao.feature.habits.ui.HabitsScreen
import dagger.hilt.android.AndroidEntryPoint

/**
 * Simple navigation state for the app
 */
private sealed class Screen {
    data object HabitsList : Screen()

    data object CreateHabit : Screen()

    data class EditHabit(val habitId: String) : Screen()
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HabitaoTheme {
                var currentScreen by remember { mutableStateOf<Screen>(Screen.HabitsList) }

                when (val screen = currentScreen) {
                    Screen.HabitsList -> {
                        HabitsScreen(
                            onAddHabit = { currentScreen = Screen.CreateHabit },
                            onEditHabit = { habitId -> currentScreen = Screen.EditHabit(habitId) },
                        )
                    }
                    Screen.CreateHabit -> {
                        CreateHabitScreen(
                            onNavigateBack = { currentScreen = Screen.HabitsList },
                            onHabitCreated = { currentScreen = Screen.HabitsList },
                        )
                    }
                    is Screen.EditHabit -> {
                        CreateHabitScreen(
                            onNavigateBack = { currentScreen = Screen.HabitsList },
                            onHabitCreated = { currentScreen = Screen.HabitsList },
                            habitId = screen.habitId,
                        )
                    }
                }
            }
        }
    }
}
