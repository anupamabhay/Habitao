package com.habitao.feature.pomodoro.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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

    var workDuration by remember { mutableIntStateOf(prefs.workDurationMinutes) }
    var shortBreakDuration by remember { mutableIntStateOf(prefs.shortBreakDurationMinutes) }
    var longBreakDuration by remember { mutableIntStateOf(prefs.longBreakDurationMinutes) }
    var sessionsBeforeLongBreak by remember { mutableIntStateOf(prefs.sessionsBeforeLongBreak) }
    var totalSessions by remember { mutableIntStateOf(prefs.totalSessions) }
    var autoStartNextPomo by remember { mutableStateOf(prefs.autoStartNextPomo) }
    var autoStartBreak by remember { mutableStateOf(prefs.autoStartBreak) }
    var autoPomoCycle by remember { mutableIntStateOf(prefs.autoPomoCycle) }
    var pomoEndingSoundUri by remember { mutableStateOf(prefs.pomoEndingSoundUri) }
    var breakEndingSoundUri by remember { mutableStateOf(prefs.breakEndingSoundUri) }
    var vibrateEnabled by remember { mutableStateOf(prefs.vibrateEnabled) }
    var vibrateDurationSeconds by remember { mutableIntStateOf(prefs.vibrateDurationSeconds) }

    val soundOptions = remember {
        listOf(
            SoundOption(
                label = "Default Alarm",
                uri = "content://settings/system/alarm_alert",
            ),
            SoundOption(
                label = "Default Notification",
                uri = "content://settings/system/notification_sound",
            ),
            SoundOption(
                label = "Default Ringtone",
                uri = "content://settings/system/ringtone",
            ),
        )
    }

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

            SettingsNumberInput(
                label = "Work Duration",
                value = workDuration,
                onValueChange = { workDuration = it },
                valueRange = 1..180,
                suffix = "min"
            )

            SettingsNumberInput(
                label = "Short Break",
                value = shortBreakDuration,
                onValueChange = { shortBreakDuration = it },
                valueRange = 1..60,
                suffix = "min"
            )

            SettingsNumberInput(
                label = "Long Break",
                value = longBreakDuration,
                onValueChange = { longBreakDuration = it },
                valueRange = 1..120,
                suffix = "min"
            )

            SettingsNumberInput(
                label = "Sessions before Long Break",
                value = sessionsBeforeLongBreak,
                onValueChange = { sessionsBeforeLongBreak = it },
                valueRange = 1..10,
                suffix = ""
            )

            SettingsNumberInput(
                label = "Total Work Sessions",
                value = totalSessions,
                onValueChange = { totalSessions = it },
                valueRange = 1..20,
                suffix = ""
            )

            SettingsSwitchRow(
                label = "Auto-start next pomodoro",
                checked = autoStartNextPomo,
                onCheckedChange = { autoStartNextPomo = it },
            )

            SettingsSwitchRow(
                label = "Auto-start break",
                checked = autoStartBreak,
                onCheckedChange = { autoStartBreak = it },
            )

            SettingsNumberInput(
                label = "Auto pomodoro cycle",
                value = autoPomoCycle,
                onValueChange = { autoPomoCycle = it },
                valueRange = 1..20,
                suffix = ""
            )

            SoundSettingRow(
                label = "Pomodoro ending sound",
                selectedUri = pomoEndingSoundUri,
                options = soundOptions,
                onSelect = { pomoEndingSoundUri = it },
            )

            SoundSettingRow(
                label = "Break ending sound",
                selectedUri = breakEndingSoundUri,
                options = soundOptions,
                onSelect = { breakEndingSoundUri = it },
            )

            SettingsSwitchRow(
                label = "Vibrate on completion",
                checked = vibrateEnabled,
                onCheckedChange = { vibrateEnabled = it },
            )

            SettingsNumberInput(
                label = "Vibration duration",
                value = vibrateDurationSeconds,
                onValueChange = { vibrateDurationSeconds = it },
                valueRange = 1..10,
                suffix = "sec"
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    prefs.workDurationMinutes = workDuration
                    prefs.shortBreakDurationMinutes = shortBreakDuration
                    prefs.longBreakDurationMinutes = longBreakDuration
                    prefs.sessionsBeforeLongBreak = sessionsBeforeLongBreak
                    prefs.totalSessions = totalSessions
                    prefs.autoStartNextPomo = autoStartNextPomo
                    prefs.autoStartBreak = autoStartBreak
                    prefs.autoPomoCycle = autoPomoCycle
                    prefs.pomoEndingSoundUri = pomoEndingSoundUri
                    prefs.breakEndingSoundUri = breakEndingSoundUri
                    prefs.vibrateEnabled = vibrateEnabled
                    prefs.vibrateDurationSeconds = vibrateDurationSeconds
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
private fun SettingsSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private data class SoundOption(
    val label: String,
    val uri: String,
)

@Composable
private fun SoundSettingRow(
    label: String,
    selectedUri: String,
    options: List<SoundOption>,
    onSelect: (String) -> Unit,
) {
    val currentIndex = options.indexOfFirst { it.uri == selectedUri }.let { index ->
        if (index == -1) 0 else index
    }
    val currentLabel = options.getOrNull(currentIndex)?.label ?: options.first().label

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Button(
            onClick = {
                val nextIndex = (currentIndex + 1) % options.size
                onSelect(options[nextIndex].uri)
            },
        ) {
            Text(text = currentLabel)
        }
    }
}

@Composable
private fun SettingsNumberInput(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    valueRange: IntRange,
    suffix: String
) {
    var textValue by remember(value) { mutableStateOf(value.toString()) }
    var isError by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        OutlinedTextField(
            value = textValue,
            onValueChange = { newValue ->
                if (newValue.isEmpty()) {
                    textValue = newValue
                    isError = true
                    return@OutlinedTextField
                }
                val intValue = newValue.toIntOrNull()
                if (intValue != null) {
                    textValue = newValue
                    if (intValue in valueRange) {
                        isError = false
                        onValueChange(intValue)
                    } else {
                        isError = true
                    }
                }
            },
            modifier = Modifier.width(120.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            isError = isError,
            suffix = if (suffix.isNotEmpty()) { { Text(suffix) } } else null,
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End)
        )
    }
}
