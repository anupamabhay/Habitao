package com.habitao.feature.pomodoro.ui.components

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.habitao.feature.pomodoro.service.PomodoroPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroSettingsSheet(
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { PomodoroPreferences(context) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // State holders
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(bottom = 24.dp)
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            // Drag handle spacer
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Pomodoro Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )
            }

            SettingsGroup(title = "Duration") {
                SettingsNumberItem(
                    label = "Work Duration",
                    value = workDuration,
                    suffix = "min",
                    range = 1..180,
                    icon = Icons.Filled.Timer,
                    onValueChange = { workDuration = it }
                )
                SettingsDivider()
                SettingsNumberItem(
                    label = "Short Break",
                    value = shortBreakDuration,
                    suffix = "min",
                    range = 1..60,
                    icon = Icons.Filled.Timer,
                    onValueChange = { shortBreakDuration = it }
                )
                SettingsDivider()
                SettingsNumberItem(
                    label = "Long Break",
                    value = longBreakDuration,
                    suffix = "min",
                    range = 1..120,
                    icon = Icons.Filled.Timer,
                    onValueChange = { longBreakDuration = it }
                )
            }

            SettingsGroup(title = "Cycles") {
                SettingsNumberItem(
                    label = "Sessions before Long Break",
                    value = sessionsBeforeLongBreak,
                    suffix = "sessions",
                    range = 1..10,
                    icon = Icons.Filled.Refresh,
                    onValueChange = { sessionsBeforeLongBreak = it }
                )
                SettingsDivider()
                SettingsNumberItem(
                    label = "Total Work Sessions",
                    value = totalSessions,
                    suffix = "sessions",
                    range = 1..20,
                    icon = Icons.Filled.Refresh,
                    onValueChange = { totalSessions = it }
                )
                SettingsDivider()
                SettingsNumberItem(
                    label = "Auto-start limit",
                    value = autoPomoCycle,
                    suffix = "cycles",
                    range = 1..20,
                    icon = Icons.Filled.Refresh,
                    onValueChange = { autoPomoCycle = it }
                )
                SettingsDivider()
                SettingsSwitchItem(
                    label = "Auto-start next pomodoro",
                    checked = autoStartNextPomo,
                    icon = Icons.Filled.PlayArrow,
                    onCheckedChange = { autoStartNextPomo = it }
                )
                SettingsDivider()
                SettingsSwitchItem(
                    label = "Auto-start break",
                    checked = autoStartBreak,
                    icon = Icons.Filled.PlayArrow,
                    onCheckedChange = { autoStartBreak = it }
                )
            }

            SettingsGroup(title = "Sound & Vibration") {
                SettingsSoundItem(
                    label = "Pomodoro ending sound",
                    selectedUri = pomoEndingSoundUri,
                    icon = Icons.Filled.Notifications,
                    onSelect = { pomoEndingSoundUri = it }
                )
                SettingsDivider()
                SettingsSoundItem(
                    label = "Break ending sound",
                    selectedUri = breakEndingSoundUri,
                    icon = Icons.Filled.Notifications,
                    onSelect = { breakEndingSoundUri = it }
                )
                SettingsDivider()
                SettingsSwitchItem(
                    label = "Vibrate on completion",
                    checked = vibrateEnabled,
                    icon = Icons.Filled.Vibration,
                    onCheckedChange = { vibrateEnabled = it }
                )
                if (vibrateEnabled) {
                    SettingsDivider()
                    SettingsNumberItem(
                        label = "Vibration duration",
                        value = vibrateDurationSeconds,
                        suffix = "sec",
                        range = 1..10,
                        icon = Icons.Filled.Timer,
                        onValueChange = { vibrateDurationSeconds = it }
                    )
                }
            }

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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                Text("Save Settings")
            }
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(start = 16.dp, bottom = 8.dp)
                .fillMaxWidth()
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        thickness = 1.dp
    )
}

@Composable
private fun SettingsSwitchItem(
    label: String,
    checked: Boolean,
    icon: ImageVector? = null,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingsNumberItem(
    label: String,
    value: Int,
    suffix: String,
    range: IntRange,
    icon: ImageVector? = null,
    onValueChange: (Int) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        NumberInputDialog(
            label = label,
            initialValue = value,
            range = range,
            onDismiss = { showDialog = false },
            onConfirm = {
                onValueChange(it)
                showDialog = false
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(6.dp),
        ) {
            Text(
                text = "$value $suffix".trim(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun NumberInputDialog(
    label: String,
    initialValue: Int,
    range: IntRange,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var textFieldValue by remember { 
        mutableStateOf(
            TextFieldValue(
                text = initialValue.toString(),
                selection = TextRange(0, initialValue.toString().length)
            )
        ) 
    }
    var isError by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = label) },
        text = {
            Column {
                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = { newValue ->
                        textFieldValue = newValue
                        isError = newValue.text.toIntOrNull()?.let { it !in range } ?: true
                    },
                    isError = isError,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    supportingText = {
                        if (isError) {
                            Text("Enter a value between ${range.first} and ${range.last}")
                        }
                    }
                )
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    textFieldValue.text.toIntOrNull()?.let {
                        if (it in range) {
                            onConfirm(it)
                        } else {
                            isError = true
                        }
                    }
                },
                enabled = !isError && textFieldValue.text.isNotEmpty()
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SettingsSoundItem(
    label: String,
    selectedUri: String,
    icon: ImageVector? = null,
    onSelect: (String) -> Unit,
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            onSelect(uri?.toString() ?: "SILENT")
        }
    }

    val ringtoneName = remember(selectedUri) {
        if (selectedUri.isEmpty()) {
            "Default"
        } else if (selectedUri == "SILENT") {
            "Silent"
        } else {
            try {
                val uri = Uri.parse(selectedUri)
                val ringtone = RingtoneManager.getRingtone(context, uri)
                ringtone?.getTitle(context) ?: "Unknown"
            } catch (e: Exception) {
                "Unknown"
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION or RingtoneManager.TYPE_ALARM)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                    if (selectedUri.isNotEmpty() && selectedUri != "SILENT") {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(selectedUri))
                    }
                }
                launcher.launch(intent)
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(6.dp),
        ) {
             Text(
                text = ringtoneName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .widthIn(max = 120.dp)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}
