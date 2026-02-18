package com.habitao.feature.habits.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.habitao.feature.habits.viewmodel.HabitsIntent
import com.habitao.feature.habits.viewmodel.HabitsState
import com.habitao.feature.habits.viewmodel.HabitsViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(
    onAddHabit: () -> Unit,
    onEditHabit: (String) -> Unit = {},
    viewModel: HabitsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var pendingDeleteHabitId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddHabit) {
                Icon(Icons.Default.Add, contentDescription = "Add Habit")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        HabitsContent(
            state = state,
            pendingDeleteHabitId = pendingDeleteHabitId,
            onCompleteHabit = { habitId ->
                viewModel.processIntent(HabitsIntent.IncrementHabitProgress(habitId))
            },
            onIncrementHabit = { habitId ->
                viewModel.processIntent(HabitsIntent.IncrementHabitProgress(habitId))
            },
            onDecrementHabit = { habitId ->
                viewModel.processIntent(HabitsIntent.DecrementHabitProgress(habitId))
            },
            onDeleteHabit = { habitId ->
                pendingDeleteHabitId = habitId
                scope.launch {
                    val result =
                        snackbarHostState.showSnackbar(
                            message = "Habit deleted",
                            actionLabel = "Undo",
                            duration = SnackbarDuration.Short,
                        )
                    when (result) {
                        SnackbarResult.ActionPerformed -> {
                            pendingDeleteHabitId = null
                        }
                        SnackbarResult.Dismissed -> {
                            pendingDeleteHabitId?.let { id ->
                                viewModel.processIntent(HabitsIntent.DeleteHabit(id))
                            }
                            pendingDeleteHabitId = null
                        }
                    }
                }
            },
            onToggleChecklistItem = { habitId, itemId ->
                viewModel.processIntent(HabitsIntent.ToggleChecklistItem(habitId, itemId))
            },
            onEditHabit = onEditHabit,
            modifier = Modifier.padding(paddingValues),
        )
    }
}

@Composable
private fun HabitsContent(
    state: HabitsState,
    pendingDeleteHabitId: String? = null,
    onCompleteHabit: (String) -> Unit,
    onIncrementHabit: (String) -> Unit,
    onDecrementHabit: (String) -> Unit,
    onDeleteHabit: (String) -> Unit,
    onToggleChecklistItem: (habitId: String, itemId: String) -> Unit,
    onEditHabit: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            state.habits.isEmpty() -> {
                EmptyState(modifier = Modifier.align(Alignment.Center))
            }
            else -> {
                val displayedHabits =
                    remember(state.habits, pendingDeleteHabitId) {
                        if (pendingDeleteHabitId != null) {
                            state.habits.filter { it.id != pendingDeleteHabitId }
                        } else {
                            state.habits
                        }
                    }

                val completedCount =
                    state.habits.count { habit ->
                        state.logs[habit.id]?.isCompleted == true
                    }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        HomeHeader(
                            date = state.selectedDate,
                            totalHabits = state.habits.size,
                            completedHabits = completedCount,
                        )
                    }

                    items(
                        items = displayedHabits,
                        key = { it.id },
                    ) { habit ->
                        HabitCard(
                            habit = habit,
                            log = state.logs[habit.id],
                            streakCount = 0, // TODO: Calculate streak
                            onComplete = { onCompleteHabit(habit.id) },
                            onUncomplete = { onDecrementHabit(habit.id) },
                            onIncrement = { onIncrementHabit(habit.id) },
                            onTap = { onEditHabit(habit.id) },
                            onDelete = { onDeleteHabit(habit.id) },
                            onToggleChecklistItem = { itemId ->
                                onToggleChecklistItem(habit.id, itemId)
                            },
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    date: LocalDate,
    totalHabits: Int,
    completedHabits: Int,
    modifier: Modifier = Modifier,
) {
    val greeting =
        remember {
            val hour = java.time.LocalTime.now().hour
            when {
                hour < 12 -> "Good morning"
                hour < 17 -> "Good afternoon"
                else -> "Good evening"
            }
        }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = greeting,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text =
                date.format(
                    java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d"),
                ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (totalHabits > 0) {
            Spacer(modifier = Modifier.height(16.dp))

            // Progress row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LinearProgressIndicator(
                    progress = {
                        if (totalHabits > 0) {
                            completedHabits.toFloat() / totalHabits
                        } else {
                            0f
                        }
                    },
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round,
                )

                Text(
                    text = "$completedHabits of $totalHabits",
                    style = MaterialTheme.typography.labelLarge,
                    color =
                        if (completedHabits == totalHabits && totalHabits > 0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "No habits yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Tap the + button to create your first habit",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
