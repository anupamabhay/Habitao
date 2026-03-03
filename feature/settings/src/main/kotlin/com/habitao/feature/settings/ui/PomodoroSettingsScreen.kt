package com.habitao.feature.settings.ui

import androidx.compose.runtime.Composable
import com.habitao.feature.pomodoro.ui.components.PomodoroSettingsSheet

@Composable
fun PomodoroSettingsScreen(onNavigateBack: () -> Unit) {
    PomodoroSettingsSheet(onDismiss = onNavigateBack)
}
