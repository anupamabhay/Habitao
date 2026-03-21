package com.habitao.feature.habits.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import com.habitao.core.ui.theme.AppShapes
import com.habitao.core.ui.theme.Dimensions
import com.habitao.feature.habits.viewmodel.HabitsIntent
import com.habitao.feature.habits.viewmodel.HabitsState
import com.habitao.feature.habits.viewmodel.HabitsViewModel
import com.habitao.feature.habits.viewmodel.SortOption
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private const val DAYS_PER_WEEK = 7L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(
    onAddHabit: () -> Unit,
    onEditHabit: (String) -> Unit = {},
    viewModel: HabitsViewModel = koinViewModel(),
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
            val hour = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour
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
            TopAppBar(
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
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            androidx.compose.material3.FloatingActionButton(
                onClick = onAddHabit,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Habit")
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
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(
                                start = Dimensions.screenPaddingHorizontal,
                                top = Dimensions.elementSpacing,
                                end = Dimensions.screenPaddingHorizontal,
                            ),
                ) {
                    HabitsListHeader(
                        selectedDate = state.selectedDate,
                        onSelectDate = onSelectDate,
                        totalHabits = 0,
                        completedHabits = 0,
                    )
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        EmptyState(
                            modifier = Modifier.offset(y = (-48).dp),
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
                        PaddingValues(
                            start = Dimensions.screenPaddingHorizontal,
                            top = Dimensions.elementSpacing,
                            end = Dimensions.screenPaddingHorizontal,
                            bottom = Dimensions.fabClearance,
                        ),
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
                            weeklyProgress = state.weeklyProgress[habit.id],
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

                    // FAB clearance is handled by LazyColumn's contentPadding.bottom
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
                selectedDate.toLongLabel(),
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
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    val pageCount = 10000
    val middlePage = pageCount / 2
    val pagerState = rememberPagerState(initialPage = middlePage, pageCount = { pageCount })
    val baseWeekStart = remember(today) { today.minus(DatePeriod(days = today.dayOfWeek.ordinal)) }
    val currentSelectedDate by rememberUpdatedState(selectedDate)

    // Sync date when pager settles on a new page — debounced and deduplicated
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                // Only update if pager is not actively being scrolled
                if (!pagerState.isScrollInProgress) {
                    val weekOffset = (page - middlePage).toLong()
                    val newWeekStart = baseWeekStart.plus(weekOffset * DAYS_PER_WEEK, kotlinx.datetime.DateTimeUnit.DAY)
                    val dayOfWeekIndex = currentSelectedDate.dayOfWeek.ordinal
                    val newDate = newWeekStart.plus(dayOfWeekIndex.toLong(), kotlinx.datetime.DateTimeUnit.DAY)
                    if (newDate != currentSelectedDate) {
                        onDateSelected(newDate)
                    }
                }
            }
    }

    HorizontalPager(
        modifier = modifier.fillMaxWidth(),
        state = pagerState,
        beyondViewportPageCount = 1,
    ) { page ->
        val weekStart = baseWeekStart.plus((page - middlePage).toLong() * DAYS_PER_WEEK, kotlinx.datetime.DateTimeUnit.DAY)
        val isCurrentWeek = page == middlePage
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimensions.elementSpacingSmall),
        ) {
            repeat(7) { dayIndex ->
                val date = weekStart.plus(dayIndex.toLong(), kotlinx.datetime.DateTimeUnit.DAY)
                DateChip(
                    date = date,
                    isSelected = date == selectedDate,
                    isToday = date == today,
                    isInCurrentWeek = isCurrentWeek,
                    onClick = { onDateSelected(date) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DateChip(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    isInCurrentWeek: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor =
        if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else if (isInCurrentWeek) {
            MaterialTheme.colorScheme.surfaceVariant
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
        modifier = modifier.height(68.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(Dimensions.elementSpacingSmall),
        ) {
            Text(
                text =
                    date.dayOfWeek.toShortLabel(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(2.dp))
            if (isToday) {
                Box(
                    modifier =
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                            ),
                )
            }
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

private fun LocalDate.toLongLabel(): String =
    "${dayOfWeek.toFullLabel()}, ${month.toFullLabel()} $dayOfMonth"

private fun DayOfWeek.toShortLabel(): String = name.take(3).lowercase().replaceFirstChar { it.uppercase() }

private fun DayOfWeek.toFullLabel(): String = name.lowercase().replaceFirstChar { it.uppercase() }

private fun kotlinx.datetime.Month.toFullLabel(): String = name.lowercase().replaceFirstChar { it.uppercase() }

private fun SortOption.displayName(): String =
    when (this) {
        SortOption.MANUAL -> "Manual"
        SortOption.ALPHABETICAL -> "A - Z"
        SortOption.NEWEST_FIRST -> "Newest first"
        SortOption.OLDEST_FIRST -> "Oldest first"
        SortOption.BY_COMPLETION -> "Incomplete first"
    }

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
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
    }
}
