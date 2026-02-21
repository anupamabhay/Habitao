package com.habitao.feature.pomodoro.ui

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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.habitao.domain.model.PomodoroType
import com.habitao.feature.pomodoro.service.TimerState
import com.habitao.feature.pomodoro.ui.components.PomodoroSettingsSheet
import com.habitao.feature.pomodoro.ui.components.TimerDisplay
import com.habitao.feature.pomodoro.viewmodel.PomodoroIntent
import com.habitao.feature.pomodoro.viewmodel.PomodoroViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroScreen(
    viewModel: PomodoroViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showStopDialog by remember { mutableStateOf(false) }
    var showSkipDialog by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    val sessionLabel =
        when (state.currentSessionType) {
            PomodoroType.WORK -> "Focus Time"
            PomodoroType.SHORT_BREAK -> "Short Break"
            PomodoroType.LONG_BREAK -> "Long Break"
        }
    val accentColor =
        if (state.currentSessionType == PomodoroType.WORK) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.tertiary
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Pomodoro") },
                actions = {
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
        ) {
            Text(
                text = sessionLabel,
                style = MaterialTheme.typography.titleMedium,
                color = accentColor,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(modifier = Modifier.height(24.dp))

            TimerDisplay(
                remainingSeconds = state.remainingSeconds,
                totalSeconds = state.totalSeconds,
                sessionType = state.currentSessionType,
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (state.currentSessionType != PomodoroType.LONG_BREAK) {
                val currentSession = (state.totalCompletedWorkSessions + 1).coerceAtMost(state.totalSessions)
                Text(
                    text = "Session $currentSession of ${state.totalSessions}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = "Final Break",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

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

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        text = "Today's Focus",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "${state.todaysFocusMinutes} min",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Focus time",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = state.todaysSessions.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Sessions",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
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
            FilledTonalButton(
                onClick = onStart,
                modifier = modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(text = "Start")
            }
        }
        TimerState.RUNNING -> {
            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onStop) {
                    Icon(
                        imageVector = Icons.Filled.Stop,
                        contentDescription = "Stop",
                    )
                }
                FilledTonalButton(
                    onClick = onPause,
                    colors =
                        ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Pause,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = "Pause")
                }
                IconButton(onClick = onSkip) {
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
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onStop) {
                    Icon(
                        imageVector = Icons.Filled.Stop,
                        contentDescription = "Stop",
                    )
                }
                FilledTonalButton(
                    onClick = onResume,
                    colors =
                        ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = "Resume")
                }
                IconButton(onClick = onSkip) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Skip",
                    )
                }
            }
        }
        TimerState.FINISHED -> {
            FilledTonalButton(
                onClick = onStart,
                modifier = modifier.fillMaxWidth(),
            ) {
                Text(text = "Start Next")
            }
        }
    }
}
