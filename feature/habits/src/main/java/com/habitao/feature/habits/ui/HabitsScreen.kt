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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.habitao.core.ui.theme.AppShapes
import com.habitao.core.ui.theme.Dimensions
import com.habitao.feature.habits.viewmodel.HabitsIntent
import com.habitao.feature.habits.viewmodel.HabitsState
import com.habitao.feature.habits.viewmodel.HabitsViewModel
import com.habitao.feature.habits.viewmodel.SortOption
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

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
    var pendingDeleteIds by remember { mutableStateOf(emptySet<String>()) }

    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val greeting =
        remember {
            val hour = java.time.LocalTime.now().hour
            when {
                hour < 12 -> "Good morning"
                hour < 17 -> "Good afternoon"
                else -> "Good evening"
            }
        }

    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(greeting) },
                actions = {
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                imageVector = Icons.Outlined.Sort,
                                contentDescription = "Sort habits",
                            )
                        }
                        SortDropdownMenu(
                            expanded = showSortMenu,
                            currentSort = state.sortOption,
                            onSortSelected = { option ->
                                viewModel.processIntent(HabitsIntent.SetSortOption(option))
                                showSortMenu = false
                            },
                            onDismiss = { showSortMenu = false },
                        )
                    }
                    IconButton(onClick = { /* Settings placeholder */ }) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddHabit,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(Dimensions.elementSpacing))
                Text("New Habit")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        HabitsContent(
            state = state,
            pendingDeleteIds = pendingDeleteIds,
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
                pendingDeleteIds = pendingDeleteIds + habitId
                scope.launch {
                    val result =
                        snackbarHostState.showSnackbar(
                            message = "Habit deleted",
                            actionLabel = "Undo",
                            duration = SnackbarDuration.Short,
                        )
                    when (result) {
                        SnackbarResult.ActionPerformed -> {
                            pendingDeleteIds = pendingDeleteIds - habitId
                        }
                        SnackbarResult.Dismissed -> {
                            viewModel.processIntent(HabitsIntent.DeleteHabit(habitId))
                            pendingDeleteIds = pendingDeleteIds - habitId
                        }
                    }
                }
            },
            onToggleChecklistItem = { habitId, itemId ->
                viewModel.processIntent(HabitsIntent.ToggleChecklistItem(habitId, itemId))
            },
            onEditHabit = onEditHabit,
            onSelectDate = { date ->
                viewModel.processIntent(HabitsIntent.SelectDate(date))
            },
            onAddHabit = onAddHabit,
            modifier = Modifier.padding(paddingValues),
        )
    }
}

@Composable
private fun HabitsContent(
    state: HabitsState,
    pendingDeleteIds: Set<String> = emptySet(),
    onCompleteHabit: (String) -> Unit,
    onIncrementHabit: (String) -> Unit,
    onDecrementHabit: (String) -> Unit,
    onDeleteHabit: (String) -> Unit,
    onToggleChecklistItem: (habitId: String, itemId: String) -> Unit,
    onEditHabit: (String) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onAddHabit: () -> Unit,
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
                Column(modifier = Modifier.fillMaxSize()) {
                    HabitsListHeader(
                        selectedDate = state.selectedDate,
                        onSelectDate = onSelectDate,
                        totalHabits = 0,
                        completedHabits = 0,
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        EmptyState(
                            onAddHabit = onAddHabit,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }
            }
            else -> {
                val displayedHabits =
                    remember(state.habits, pendingDeleteIds) {
                        if (pendingDeleteIds.isNotEmpty()) {
                            state.habits.filter { it.id !in pendingDeleteIds }
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
                    contentPadding =
                        PaddingValues(horizontal = Dimensions.screenPaddingHorizontal),
                    verticalArrangement =
                        Arrangement.spacedBy(Dimensions.cardSpacing),
                ) {
                    item {
                        HabitsListHeader(
                            selectedDate = state.selectedDate,
                            onSelectDate = onSelectDate,
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
                            streakCount = state.streaks[habit.id] ?: 0,
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
                        Spacer(modifier = Modifier.height(Dimensions.fabClearance))
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitsListHeader(
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    totalHabits: Int,
    completedHabits: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = Dimensions.screenPaddingVertical),
    ) {
        Text(
            text =
                selectedDate.format(
                    DateTimeFormatter.ofPattern("EEEE, MMMM d"),
                ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = Dimensions.elementSpacing),
        )

        DateSelector(
            selectedDate = selectedDate,
            onDateSelected = onSelectDate,
            modifier = Modifier.padding(bottom = Dimensions.sectionSpacing),
        )

        if (totalHabits > 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement =
                    Arrangement.spacedBy(Dimensions.elementSpacingLarge),
            ) {
                LinearProgressIndicator(
                    progress = { completedHabits.toFloat() / totalHabits },
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(Dimensions.progressBarHeightLarge)
                            .clip(AppShapes.progressBar),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round,
                )

                Text(
                    text = "$completedHabits of $totalHabits",
                    style = MaterialTheme.typography.labelLarge,
                    color =
                        if (completedHabits == totalHabits) {
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
private fun DateSelector(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = remember { LocalDate.now() }
    val dates =
        remember(today) {
            (-3..3).map { today.plusDays(it.toLong()) }
        }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.spacedBy(Dimensions.elementSpacing),
    ) {
        items(dates) { date ->
            DateChip(
                date = date,
                isSelected = date == selectedDate,
                onClick = { onDateSelected(date) },
            )
        }
    }
}

@Composable
private fun DateChip(
    date: LocalDate,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val backgroundColor =
        if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        }
    val contentColor =
        if (isSelected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        }

    Surface(
        onClick = onClick,
        shape = AppShapes.dateChip,
        color = backgroundColor,
        contentColor = contentColor,
        modifier =
            Modifier
                .width(48.dp)
                .height(64.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(4.dp),
        ) {
            Text(
                text =
                    date.dayOfWeek.getDisplayName(
                        TextStyle.SHORT,
                        Locale.getDefault(),
                    ),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SortDropdownMenu(
    expanded: Boolean,
    currentSort: SortOption,
    onSortSelected: (SortOption) -> Unit,
    onDismiss: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
    ) {
        SortOption.entries.forEach { option ->
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement =
                            Arrangement.spacedBy(Dimensions.elementSpacing),
                    ) {
                        RadioButton(
                            selected = currentSort == option,
                            onClick = null,
                        )
                        Text(
                            text = option.displayName(),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                },
                onClick = { onSortSelected(option) },
            )
        }
    }
}

private fun SortOption.displayName(): String =
    when (this) {
        SortOption.MANUAL -> "Manual"
        SortOption.ALPHABETICAL -> "A - Z"
        SortOption.NEWEST_FIRST -> "Newest first"
        SortOption.OLDEST_FIRST -> "Oldest first"
        SortOption.BY_COMPLETION -> "Incomplete first"
    }

@Composable
private fun EmptyState(
    onAddHabit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.Checklist,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
        )

        Spacer(modifier = Modifier.height(Dimensions.sectionSpacing))

        Text(
            text = "No habits yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(Dimensions.elementSpacing))

        Text(
            text = "Start building better routines",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(Dimensions.sectionSpacing))

        OutlinedButton(onClick = onAddHabit) {
            Text("Create your first habit")
        }
    }
}
