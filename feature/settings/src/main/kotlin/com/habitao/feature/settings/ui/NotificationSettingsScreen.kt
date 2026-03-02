package com.habitao.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import com.habitao.core.ui.theme.Dimensions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onNavigateBack: () -> Unit,
    habitRemindersEnabled: Boolean,
    taskRemindersEnabled: Boolean,
    pomodoroNotificationsEnabled: Boolean,
    onHabitRemindersEnabledChanged: (Boolean) -> Unit,
    onTaskRemindersEnabledChanged: (Boolean) -> Unit,
    onPomodoroNotificationsEnabledChanged: (Boolean) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
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
                text = "Reminders",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )

            ListItem(
                headlineContent = { Text("Habit Reminders") },
                supportingContent = { Text("Receive notifications for scheduled habits") },
                trailingContent = {
                    Switch(
                        checked = habitRemindersEnabled,
                        onCheckedChange = onHabitRemindersEnabledChanged,
                    )
                },
            )

            ListItem(
                headlineContent = { Text("Task Reminders") },
                supportingContent = { Text("Receive notifications for tasks with due dates") },
                trailingContent = {
                    Switch(
                        checked = taskRemindersEnabled,
                        onCheckedChange = onTaskRemindersEnabledChanged,
                    )
                },
            )

            HorizontalDivider()

            Text(
                text = "Pomodoro",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )

            ListItem(
                headlineContent = { Text("Session Complete") },
                supportingContent = { Text("Notify when a focus session or break ends") },
                trailingContent = {
                    Switch(
                        checked = pomodoroNotificationsEnabled,
                        onCheckedChange = onPomodoroNotificationsEnabledChanged,
                    )
                },
            )
        }
    }
}
