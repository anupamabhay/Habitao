package com.habitao.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.habitao.core.ui.theme.Dimensions
import com.habitao.feature.pomodoro.service.PomodoroPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PomodoroSettingsViewModel
    @Inject
    constructor(
        val pomodoroPreferences: PomodoroPreferences,
    ) : ViewModel()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: PomodoroSettingsViewModel = hiltViewModel(),
) {
    val prefs = viewModel.pomodoroPreferences
    var workMinutes by remember { mutableIntStateOf(prefs.workDurationMinutes) }
    var shortBreakMinutes by remember { mutableIntStateOf(prefs.shortBreakDurationMinutes) }
    var longBreakMinutes by remember { mutableIntStateOf(prefs.longBreakDurationMinutes) }
    var sessionsBeforeLong by remember { mutableIntStateOf(prefs.sessionsBeforeLongBreak) }
    var totalSessions by remember { mutableIntStateOf(prefs.totalSessions) }
    var autoStartNext by remember { mutableStateOf(prefs.autoStartNextPomo) }
    var autoStartBreak by remember { mutableStateOf(prefs.autoStartBreak) }
    var vibrateEnabled by remember { mutableStateOf(prefs.vibrateEnabled) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Focus & Pomodoro") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = Dimensions.screenPaddingHorizontal),
            verticalArrangement = Arrangement.spacedBy(Dimensions.elementSpacing),
        ) {
            Spacer(modifier = Modifier.height(Dimensions.elementSpacing))

            Text(
                "Durations",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )

            DurationSetting("Focus", workMinutes) {
                workMinutes = it
                prefs.workDurationMinutes = it
            }
            DurationSetting("Short Break", shortBreakMinutes) {
                shortBreakMinutes = it
                prefs.shortBreakDurationMinutes = it
            }
            DurationSetting("Long Break", longBreakMinutes) {
                longBreakMinutes = it
                prefs.longBreakDurationMinutes = it
            }

            HorizontalDivider()

            Text(
                "Sessions",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )

            DurationSetting("Sessions before long break", sessionsBeforeLong) {
                sessionsBeforeLong = it
                prefs.sessionsBeforeLongBreak = it
            }
            DurationSetting("Total sessions", totalSessions) {
                totalSessions = it
                prefs.totalSessions = it
            }

            HorizontalDivider()

            Text(
                "Behavior",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )

            ListItem(
                headlineContent = { Text("Auto-start next session") },
                trailingContent = {
                    Switch(
                        checked = autoStartNext,
                        onCheckedChange = {
                            autoStartNext = it
                            prefs.autoStartNextPomo = it
                        },
                    )
                },
            )
            ListItem(
                headlineContent = { Text("Auto-start break") },
                trailingContent = {
                    Switch(
                        checked = autoStartBreak,
                        onCheckedChange = {
                            autoStartBreak = it
                            prefs.autoStartBreak = it
                        },
                    )
                },
            )
            ListItem(
                headlineContent = { Text("Vibrate on complete") },
                trailingContent = {
                    Switch(
                        checked = vibrateEnabled,
                        onCheckedChange = {
                            vibrateEnabled = it
                            prefs.vibrateEnabled = it
                        },
                    )
                },
            )
        }
    }
}

@Composable
private fun DurationSetting(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledTonalIconButton(
                onClick = { if (value > 1) onValueChange(value - 1) },
                modifier = Modifier.size(32.dp),
            ) {
                Text("-", fontWeight = FontWeight.Bold)
            }
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            FilledTonalIconButton(
                onClick = { onValueChange(value + 1) },
                modifier = Modifier.size(32.dp),
            ) {
                Text("+", fontWeight = FontWeight.Bold)
            }
        }
    }
}
