package com.habitao.feature.pomodoro.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.habitao.domain.model.PomodoroType
import com.habitao.feature.pomodoro.service.TimerState
import com.habitao.feature.pomodoro.ui.components.PomodoroSettingsSheet
import com.habitao.feature.pomodoro.ui.components.TimerDisplay
import com.habitao.feature.pomodoro.viewmodel.FocusLinkOption
import com.habitao.feature.pomodoro.viewmodel.FocusLinkType
import com.habitao.feature.pomodoro.viewmodel.PomodoroIntent
import com.habitao.feature.pomodoro.viewmodel.PomodoroViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroScreen(
    viewModel: PomodoroViewModel = hiltViewModel(),
    onOpenFullScreen: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    var showStopDialog by remember { mutableStateOf(false) }
    var showSkipDialog by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showFocusSelector by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { /* Empty title to match TickTick */ },
                actions = {
                    IconButton(onClick = onOpenFullScreen) {
                        Icon(
                            imageVector = Icons.Filled.Fullscreen,
                            contentDescription = "Full Screen",
                        )
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                        )
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
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            TextButton(
                onClick = { showFocusSelector = true },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
            ) {
                val focusText = state.selectedFocusOption?.title ?: "Focus"
                Text(
                    text = "$focusText >",
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            TimerDisplay(
                remainingSeconds = state.remainingSeconds,
                totalSeconds = state.totalSeconds,
                sessionType = state.currentSessionType,
                timerState = state.timerState,
                modifier = Modifier.size(280.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            TimerControls(
                timerState = state.timerState,
                onStart = { viewModel.processIntent(PomodoroIntent.StartTimer) },
                onPause = { viewModel.processIntent(PomodoroIntent.PauseTimer) },
                onResume = { viewModel.processIntent(PomodoroIntent.ResumeTimer) },
                onStop = { showStopDialog = true },
                onSkip = { showSkipDialog = true },
            )

            if (showStopDialog) {
                AlertDialog(
                    onDismissRequest = { showStopDialog = false },
                    title = { Text(text = "Stop Timer?") },
                    text = { Text(text = "Your progress will be saved but marked as interrupted.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showStopDialog = false
                                viewModel.processIntent(PomodoroIntent.StopTimer)
                            },
                        ) {
                            Text(text = "Stop")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showStopDialog = false }) {
                            Text(text = "Cancel")
                        }
                    },
                )
            }

            if (showSkipDialog) {
                AlertDialog(
                    onDismissRequest = { showSkipDialog = false },
                    title = { Text(text = "Skip to Next?") },
                    text = { Text(text = "Current session will end and move to next.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showSkipDialog = false
                                viewModel.processIntent(PomodoroIntent.SkipToNext)
                            },
                        ) {
                            Text(text = "Skip")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSkipDialog = false }) {
                            Text(text = "Cancel")
                        }
                    },
                )
            }

            if (showSettings) {
                PomodoroSettingsSheet(onDismiss = { showSettings = false })
            }

            if (showFocusSelector) {
                FocusSelectionSheet(
                    activeTasks = state.activeTaskOptions,
                    activeHabits = state.activeHabitOptions,
                    selectedFocusOption = state.selectedFocusOption,
                    onSelectTask = { taskId ->
                        viewModel.processIntent(PomodoroIntent.LinkTask(taskId))
                        showFocusSelector = false
                    },
                    onSelectHabit = { habitId ->
                        viewModel.processIntent(PomodoroIntent.LinkHabit(habitId))
                        showFocusSelector = false
                    },
                    onClearSelection = {
                        viewModel.processIntent(PomodoroIntent.ClearLinkedFocus)
                        showFocusSelector = false
                    },
                    onDismiss = { showFocusSelector = false },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FocusSelectionSheet(
    activeTasks: List<FocusLinkOption>,
    activeHabits: List<FocusLinkOption>,
    selectedFocusOption: FocusLinkOption?,
    onSelectTask: (String) -> Unit,
    onSelectHabit: (String) -> Unit,
    onClearSelection: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
        ) {
            Text(
                text = "Select Task/Habit to focus on",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp),
            )

            selectedFocusOption?.let {
                TextButton(
                    onClick = onClearSelection,
                    modifier = Modifier.padding(horizontal = 12.dp),
                ) {
                    Text(text = "Clear current link")
                }
            }

            if (activeTasks.isEmpty() && activeHabits.isEmpty()) {
                Text(
                    text = "No active tasks or habits available.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                )
            } else {
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                ) {
                    if (activeTasks.isNotEmpty()) {
                        item {
                            FocusSectionHeader(title = "Tasks")
                        }

                        items(
                            items = activeTasks,
                            key = { task -> "task-${task.id}" },
                        ) { task ->
                            FocusSelectionRow(
                                option = task,
                                selected = selectedFocusOption?.type == FocusLinkType.TASK && selectedFocusOption.id == task.id,
                                onClick = { onSelectTask(task.id) },
                            )
                        }
                    }

                    if (activeHabits.isNotEmpty()) {
                        item {
                            FocusSectionHeader(title = "Habits")
                        }

                        items(
                            items = activeHabits,
                            key = { habit -> "habit-${habit.id}" },
                        ) { habit ->
                            FocusSelectionRow(
                                option = habit,
                                selected = selectedFocusOption?.type == FocusLinkType.HABIT && selectedFocusOption.id == habit.id,
                                onClick = { onSelectHabit(habit.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FocusSectionHeader(title: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun FocusSelectionRow(
    option: FocusLinkOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val icon =
            when (option.type) {
                FocusLinkType.TASK -> Icons.Outlined.TaskAlt
                FocusLinkType.HABIT -> Icons.Outlined.CheckCircleOutline
            }

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = option.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = option.type.displayName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private val FocusLinkType.displayName: String
    get() =
        when (this) {
            FocusLinkType.TASK -> "Task"
            FocusLinkType.HABIT -> "Habit"
        }

@Composable
private fun TimerControls(
    timerState: TimerState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (timerState) {
        TimerState.IDLE -> {
            Button(
                onClick = onStart,
                modifier = modifier
                    .width(180.dp)
                    .height(48.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Start",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        TimerState.RUNNING -> {
            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onStop, modifier = Modifier.padding(end = 16.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Stop,
                        contentDescription = "Stop",
                    )
                }
                Button(
                    onClick = onPause,
                    modifier = Modifier
                        .width(180.dp)
                        .height(48.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text(
                        text = "Pause",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                IconButton(onClick = onSkip, modifier = Modifier.padding(start = 16.dp)) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Skip",
                    )
                }
            }
        }
        TimerState.PAUSED -> {
            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onStop, modifier = Modifier.padding(end = 16.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Stop,
                        contentDescription = "Stop",
                    )
                }
                Button(
                    onClick = onResume,
                    modifier = Modifier
                        .width(180.dp)
                        .height(48.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text(
                        text = "Resume",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                IconButton(onClick = onSkip, modifier = Modifier.padding(start = 16.dp)) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Skip",
                    )
                }
            }
        }
        TimerState.FINISHED -> {
            Button(
                onClick = onStart,
                modifier = modifier
                    .width(180.dp)
                    .height(48.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Start Next",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
