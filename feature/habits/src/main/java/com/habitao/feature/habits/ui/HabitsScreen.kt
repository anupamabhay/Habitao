package com.habitao.feature.habits.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.habitao.domain.model.Habit
import com.habitao.feature.habits.viewmodel.HabitsIntent
import com.habitao.feature.habits.viewmodel.HabitsState
import com.habitao.feature.habits.viewmodel.HabitsViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(
    onAddHabit: () -> Unit,
    onEditHabit: (String) -> Unit = {},
    viewModel: HabitsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Habits") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddHabit) {
                Icon(Icons.Default.Add, contentDescription = "Add Habit")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        HabitsContent(
            state = state,
            onCompleteHabit = { habitId ->
                viewModel.processIntent(HabitsIntent.IncrementHabitProgress(habitId))
            },
            onIncrementHabit = { habitId ->
                viewModel.processIntent(HabitsIntent.IncrementHabitProgress(habitId))
            },
            onDeleteHabit = { habitId ->
                viewModel.processIntent(HabitsIntent.DeleteHabit(habitId))
            },
            onEditHabit = onEditHabit,
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
private fun HabitsContent(
    state: HabitsState,
    onCompleteHabit: (String) -> Unit,
    onIncrementHabit: (String) -> Unit,
    onDeleteHabit: (String) -> Unit,
    onEditHabit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            state.habits.isEmpty() -> {
                EmptyState(modifier = Modifier.align(Alignment.Center))
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        DateHeader(date = state.selectedDate)
                    }

                    items(
                        items = state.habits,
                        key = { it.id }
                    ) { habit ->
                        HabitCard(
                            habit = habit,
                            log = state.logs[habit.id],
                            streakCount = 0, // TODO: Calculate streak
                            onComplete = { onCompleteHabit(habit.id) },
                            onIncrement = { onIncrementHabit(habit.id) },
                            onTap = { onEditHabit(habit.id) },
                            onDelete = { onDeleteHabit(habit.id) }
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
private fun DateHeader(
    date: LocalDate,
    modifier: Modifier = Modifier
) {
    val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d")
    val dateText = date.format(formatter)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = dateText,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No habits yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Tap the + button to create your first habit",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}


