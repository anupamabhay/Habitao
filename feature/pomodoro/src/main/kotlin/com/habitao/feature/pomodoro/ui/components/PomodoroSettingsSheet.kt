package com.habitao.feature.pomodoro.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.habitao.feature.pomodoro.service.PomodoroPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroSettingsSheet(
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { PomodoroPreferences(context) }
    val sheetState = rememberModalBottomSheetState()

    var workDuration by remember { mutableFloatStateOf(prefs.workDurationMinutes.toFloat()) }
    var shortBreakDuration by remember { mutableFloatStateOf(prefs.shortBreakDurationMinutes.toFloat()) }
    var longBreakDuration by remember { mutableFloatStateOf(prefs.longBreakDurationMinutes.toFloat()) }
    var sessionsBeforeLongBreak by remember { mutableFloatStateOf(prefs.sessionsBeforeLongBreak.toFloat()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Timer Settings",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            SettingsSlider(
                label = "Work Duration",
                value = workDuration,
                onValueChange = { workDuration = it },
                valueRange = 1f..60f,
                displayValue = "${workDuration.toInt()} min"
            )

            SettingsSlider(
                label = "Short Break",
                value = shortBreakDuration,
                onValueChange = { shortBreakDuration = it },
                valueRange = 1f..30f,
                displayValue = "${shortBreakDuration.toInt()} min"
            )

            SettingsSlider(
                label = "Long Break",
                value = longBreakDuration,
                onValueChange = { longBreakDuration = it },
                valueRange = 1f..60f,
                displayValue = "${longBreakDuration.toInt()} min"
            )

            SettingsSlider(
                label = "Sessions before Long Break",
                value = sessionsBeforeLongBreak,
                onValueChange = { sessionsBeforeLongBreak = it },
                valueRange = 1f..8f,
                displayValue = "${sessionsBeforeLongBreak.toInt()}"
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    prefs.workDurationMinutes = workDuration.toInt()
                    prefs.shortBreakDurationMinutes = shortBreakDuration.toInt()
                    prefs.longBreakDurationMinutes = longBreakDuration.toInt()
                    prefs.sessionsBeforeLongBreak = sessionsBeforeLongBreak.toInt()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Settings")
            }
        }
    }
}

@Composable
private fun SettingsSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    displayValue: String
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(text = displayValue, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = (valueRange.endInclusive - valueRange.start).toInt() - 1
        )
    }
}
