package com.habitao.feature.tasks.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.habitao.core.ui.theme.Dimensions
import com.habitao.domain.model.Task
import com.habitao.domain.model.TaskPriority
import com.habitao.feature.tasks.viewmodel.TaskSortOrder
import com.habitao.feature.tasks.viewmodel.TasksIntent
import com.habitao.feature.tasks.viewmodel.TasksState
import com.habitao.feature.tasks.viewmodel.TasksViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    onAddTask: () -> Unit,
    onEditTask: (String) -> Unit,
    viewModel: TasksViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var sortMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Tasks") },
                actions = {
                    Box {
                        IconButton(onClick = { sortMenuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = "Sort tasks"
                            )
                        }

                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            TaskSortOrder.values().forEach { sortOrder ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = sortOrderLabel(sortOrder),
                                            color = if (state.sortOrder == sortOrder) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    },
                                    onClick = {
                                        viewModel.processIntent(TasksIntent.SetSortOrder(sortOrder))
                                        sortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddTask,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(Dimensions.elementSpacing))
                Text("New Task")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        TasksContent(
            state = state,
            onToggleComplete = { taskId, isCompleted ->
                viewModel.processIntent(TasksIntent.ToggleComplete(taskId, isCompleted))
            },
            onTaskClick = onEditTask,
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
private fun TasksContent(
    state: TasksState,
    onToggleComplete: (String, Boolean) -> Unit,
    onTaskClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val sectionExpanded = remember {
        mutableStateMapOf(
            "Overdue" to true,
            "Today" to true,
            "Tomorrow" to true,
            "Upcoming" to true,
            "Completed" to false
        )
    }
    val expandedTaskIds = remember { mutableStateMapOf<String, Boolean>() }

    val hasActiveTasks =
        state.overdueTasks.isNotEmpty() ||
            state.todayTasks.isNotEmpty() ||
            state.tomorrowTasks.isNotEmpty() ||
            state.upcomingTasks.isNotEmpty()
    val hasCompletedTasks = state.completedTasks.isNotEmpty() || state.orphanCompletedSubTasks.isNotEmpty()

    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            !hasActiveTasks && !hasCompletedTasks -> {
                EmptyState(modifier = Modifier.align(Alignment.Center))
            }

            else -> {
                val errorColor = MaterialTheme.colorScheme.error
                val primaryColor = MaterialTheme.colorScheme.primary
                val secondaryColor = MaterialTheme.colorScheme.secondary
                val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = Dimensions.screenPaddingHorizontal,
                        top = Dimensions.elementSpacing,
                        end = Dimensions.screenPaddingHorizontal,
                        bottom = Dimensions.fabClearance
                    ),
                    verticalArrangement = Arrangement.spacedBy(Dimensions.elementSpacing)
                ) {
                    section(
                        title = "Overdue",
                        color = errorColor,
                        tasks = state.overdueTasks,
                        sectionExpanded = sectionExpanded,
                        expandedTaskIds = expandedTaskIds,
                        subTasks = state.subTasks,
                        onToggleComplete = onToggleComplete,
                        onTaskClick = onTaskClick
                    )

                    section(
                        title = "Today",
                        color = primaryColor,
                        tasks = state.todayTasks,
                        sectionExpanded = sectionExpanded,
                        expandedTaskIds = expandedTaskIds,
                        subTasks = state.subTasks,
                        onToggleComplete = onToggleComplete,
                        onTaskClick = onTaskClick
                    )

                    section(
                        title = "Tomorrow",
                        color = secondaryColor,
                        tasks = state.tomorrowTasks,
                        sectionExpanded = sectionExpanded,
                        expandedTaskIds = expandedTaskIds,
                        subTasks = state.subTasks,
                        onToggleComplete = onToggleComplete,
                        onTaskClick = onTaskClick
                    )

                    section(
                        title = "Upcoming",
                        color = onSurfaceVariantColor,
                        tasks = state.upcomingTasks,
                        sectionExpanded = sectionExpanded,
                        expandedTaskIds = expandedTaskIds,
                        subTasks = state.subTasks,
                        onToggleComplete = onToggleComplete,
                        onTaskClick = onTaskClick
                    )

                    if (hasCompletedTasks) {
                        item {
                            TaskSectionHeader(
                                title = "Completed",
                                color = onSurfaceVariantColor,
                                count = state.completedTasks.size +
                                    state.completedSubTasks.values.sumOf { it.size } +
                                    state.orphanCompletedSubTasks.size,
                                isExpanded = sectionExpanded["Completed"] ?: false,
                                onToggle = {
                                    sectionExpanded["Completed"] = !(sectionExpanded["Completed"] ?: false)
                                }
                            )
                        }

                        if (sectionExpanded["Completed"] == true) {
                            items(state.completedTasks, key = { it.id }) { task ->
                                TaskItemWithSubtasks(
                                    task = task,
                                    subtasks = state.completedSubTasks[task.id] ?: emptyList(),
                                    onToggleComplete = onToggleComplete,
                                    onTaskClick = onTaskClick,
                                    isExpanded = expandedTaskIds[task.id] ?: true,
                                    onToggleExpanded = {
                                        expandedTaskIds[task.id] = !(expandedTaskIds[task.id] ?: true)
                                    }
                                )
                            }

                            items(state.orphanCompletedSubTasks, key = { it.id }) { task ->
                                TaskRow(
                                    task = task,
                                    onToggleComplete = { onToggleComplete(task.id, it) },
                                    onClick = { onTaskClick(task.id) },
                                    isSubtask = false,
                                    showSubtaskChevron = false,
                                    onToggleExpanded = null,
                                    isExpanded = true
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.section(
    title: String,
    color: Color,
    tasks: List<Task>,
    sectionExpanded: MutableMap<String, Boolean>,
    expandedTaskIds: MutableMap<String, Boolean>,
    subTasks: Map<String, List<Task>>,
    onToggleComplete: (String, Boolean) -> Unit,
    onTaskClick: (String) -> Unit
) {
    if (tasks.isEmpty()) return

    item {
        TaskSectionHeader(
            title = title,
            color = color,
            count = tasks.size,
            isExpanded = sectionExpanded[title] ?: true,
            onToggle = {
                sectionExpanded[title] = !(sectionExpanded[title] ?: true)
            }
        )
    }

    if (sectionExpanded[title] != false) {
        items(tasks, key = { it.id }) { task ->
            TaskItemWithSubtasks(
                task = task,
                subtasks = subTasks[task.id] ?: emptyList(),
                onToggleComplete = onToggleComplete,
                onTaskClick = onTaskClick,
                isExpanded = expandedTaskIds[task.id] ?: true,
                onToggleExpanded = {
                    expandedTaskIds[task.id] = !(expandedTaskIds[task.id] ?: true)
                }
            )
        }
    }
}

@Composable
private fun TaskSectionHeader(
    title: String,
    color: Color,
    count: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onToggle)
            .padding(vertical = Dimensions.elementSpacingSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
            contentDescription = if (isExpanded) "Collapse $title" else "Expand $title",
            tint = color
        )
        Spacer(modifier = Modifier.width(Dimensions.elementSpacingSmall))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = color,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun sortOrderLabel(sortOrder: TaskSortOrder): String {
    return when (sortOrder) {
        TaskSortOrder.DATE -> "Date"
        TaskSortOrder.PRIORITY -> "Priority"
        TaskSortOrder.ALPHABETICAL -> "Alphabetical"
    }
}

@Composable
private fun TaskItemWithSubtasks(
    task: Task,
    subtasks: List<Task>,
    onToggleComplete: (String, Boolean) -> Unit,
    onTaskClick: (String) -> Unit,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit
) {
    Column {
        TaskRow(
            task = task,
            onToggleComplete = { onToggleComplete(task.id, it) },
            onClick = { onTaskClick(task.id) },
            isSubtask = false,
            showSubtaskChevron = subtasks.isNotEmpty(),
            onToggleExpanded = if (subtasks.isNotEmpty()) onToggleExpanded else null,
            isExpanded = isExpanded
        )

        if (subtasks.isNotEmpty() && isExpanded) {
            Column {
                subtasks.forEach { subtask ->
                    TaskRow(
                        task = subtask,
                        onToggleComplete = { onToggleComplete(subtask.id, it) },
                        onClick = { onTaskClick(subtask.id) },
                        isSubtask = true,
                        showSubtaskChevron = false,
                        onToggleExpanded = null,
                        isExpanded = true
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskRow(
    task: Task,
    onToggleComplete: (Boolean) -> Unit,
    onClick: () -> Unit,
    isSubtask: Boolean,
    showSubtaskChevron: Boolean,
    onToggleExpanded: (() -> Unit)?,
    isExpanded: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(
                start = if (isSubtask) 48.dp else 0.dp,
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Priority color bar on the left
        if (task.priority != TaskPriority.NONE) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(getPriorityColor(task.priority))
            )
            Spacer(modifier = Modifier.width(Dimensions.elementSpacingSmall))
        } else {
            Spacer(modifier = Modifier.width(4.dp + Dimensions.elementSpacingSmall))
        }

        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = onToggleComplete
        )

        Spacer(modifier = Modifier.width(Dimensions.elementSpacingSmall))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            task.dueDate?.let { date ->
                Text(
                    text = date.format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (showSubtaskChevron && onToggleExpanded != null) {
            Spacer(modifier = Modifier.width(Dimensions.elementSpacingSmall))
            IconButton(
                onClick = onToggleExpanded,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                    contentDescription = if (isExpanded) "Collapse subtasks" else "Expand subtasks",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun getPriorityColor(priority: TaskPriority): Color {
    return when (priority) {
        TaskPriority.HIGH -> Color(0xFFE53935)
        TaskPriority.MEDIUM -> Color(0xFFFFB300)
        TaskPriority.LOW -> Color(0xFF1E88E5)
        TaskPriority.NONE -> Color.Transparent
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.TaskAlt,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(Dimensions.sectionSpacing))

        Text(
            text = "No tasks yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
