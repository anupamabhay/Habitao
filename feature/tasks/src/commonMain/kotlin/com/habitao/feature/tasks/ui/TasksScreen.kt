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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.SubdirectoryArrowRight
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import com.habitao.core.ui.theme.Dimensions
import com.habitao.domain.model.Task
import com.habitao.domain.model.TaskPriority
import com.habitao.feature.tasks.MAX_SUBTASK_DEPTH
import com.habitao.feature.tasks.viewmodel.TaskSortOrder
import com.habitao.feature.tasks.viewmodel.TasksIntent
import com.habitao.feature.tasks.viewmodel.TasksState
import com.habitao.feature.tasks.viewmodel.TasksViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    onAddTask: () -> Unit,
    onEditTask: (String) -> Unit,
    onOpenGlobalSearch: () -> Unit,
    viewModel: TasksViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var quickAddTitle by remember { mutableStateOf("") }

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
                    IconButton(onClick = onOpenGlobalSearch) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Global search",
                        )
                    }
                    Box {
                        IconButton(onClick = { sortMenuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = "Sort tasks",
                            )
                        }

                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false },
                        ) {
                            TaskSortOrder.values().forEach { sortOrder ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = sortOrderLabel(sortOrder),
                                            color =
                                                if (state.sortOrder == sortOrder) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                },
                                        )
                                    },
                                    onClick = {
                                        viewModel.processIntent(TasksIntent.SetSortOrder(sortOrder))
                                        sortMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(Dimensions.elementSpacing),
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 2.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = quickAddTitle,
                            onValueChange = { quickAddTitle = it },
                            singleLine = true,
                            placeholder = { Text("Quick add to Inbox") },
                            modifier = Modifier.width(220.dp),
                        )
                        IconButton(
                            onClick = {
                                viewModel.processIntent(TasksIntent.QuickAddTask(quickAddTitle))
                                quickAddTitle = ""
                            },
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add to Inbox")
                        }
                    }
                }

                FloatingActionButton(
                    onClick = onAddTask,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Open full task editor")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        TasksContent(
            state = state,
            onToggleComplete = { taskId, isCompleted ->
                viewModel.processIntent(TasksIntent.ToggleComplete(taskId, isCompleted))
            },
            onTaskExpandedChange = { taskId, isExpanded ->
                viewModel.processIntent(TasksIntent.SetTaskExpanded(taskId, isExpanded))
            },
            onTaskClick = onEditTask,
            modifier = Modifier.padding(paddingValues),
        )
    }
}

@Composable
private fun TasksContent(
    state: TasksState,
    onToggleComplete: (String, Boolean) -> Unit,
    onTaskExpandedChange: (String, Boolean) -> Unit,
    onTaskClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sectionExpanded =
        remember {
            mutableStateMapOf(
                "Overdue" to true,
                "Today" to true,
                "Tomorrow" to true,
                "Upcoming" to true,
                "Inbox" to true,
                "Completed" to false,
            )
        }
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }

    val hasActiveTasks =
        state.overdueTasks.isNotEmpty() ||
            state.todayTasks.isNotEmpty() ||
            state.tomorrowTasks.isNotEmpty() ||
            state.inboxTasks.isNotEmpty() ||
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

                val totalActive =
                    state.overdueTasks.size + state.todayTasks.size +
                        state.tomorrowTasks.size + state.inboxTasks.size + state.upcomingTasks.size
                val totalCompleted = state.completedTasks.size
                val totalAll = totalActive + totalCompleted

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding =
                        PaddingValues(
                            top = Dimensions.elementSpacing,
                            bottom = Dimensions.fabClearance,
                        ),
                ) {
                    // Task overview stats
                    if (totalAll > 0) {
                        item(key = "task_overview") {
                            TaskOverviewRow(
                                totalTasks = totalAll,
                                completedTasks = totalCompleted,
                                overdueTasks = state.overdueTasks.size,
                            )
                        }
                    }

                    section(
                        title = "Overdue",
                        color = errorColor,
                        tasks = state.overdueTasks,
                        sectionExpanded = sectionExpanded,
                        expandedTaskIds = state.expandedTaskIds,
                        subTasks = state.subTasks,
                        onTaskExpandedChange = onTaskExpandedChange,
                        onToggleComplete = onToggleComplete,
                        onTaskClick = onTaskClick,
                        today = today,
                    )

                    section(
                        title = "Today",
                        color = primaryColor,
                        tasks = state.todayTasks,
                        sectionExpanded = sectionExpanded,
                        expandedTaskIds = state.expandedTaskIds,
                        subTasks = state.subTasks,
                        onTaskExpandedChange = onTaskExpandedChange,
                        onToggleComplete = onToggleComplete,
                        onTaskClick = onTaskClick,
                        today = today,
                    )

                    section(
                        title = "Tomorrow",
                        color = secondaryColor,
                        tasks = state.tomorrowTasks,
                        sectionExpanded = sectionExpanded,
                        expandedTaskIds = state.expandedTaskIds,
                        subTasks = state.subTasks,
                        onTaskExpandedChange = onTaskExpandedChange,
                        onToggleComplete = onToggleComplete,
                        onTaskClick = onTaskClick,
                        today = today,
                    )

                    section(
                        title = "Inbox",
                        color = onSurfaceVariantColor,
                        tasks = state.inboxTasks,
                        sectionExpanded = sectionExpanded,
                        expandedTaskIds = state.expandedTaskIds,
                        subTasks = state.subTasks,
                        onTaskExpandedChange = onTaskExpandedChange,
                        onToggleComplete = onToggleComplete,
                        onTaskClick = onTaskClick,
                        today = today,
                    )

                    section(
                        title = "Upcoming",
                        color = onSurfaceVariantColor,
                        tasks = state.upcomingTasks,
                        sectionExpanded = sectionExpanded,
                        expandedTaskIds = state.expandedTaskIds,
                        subTasks = state.subTasks,
                        onTaskExpandedChange = onTaskExpandedChange,
                        onToggleComplete = onToggleComplete,
                        onTaskClick = onTaskClick,
                        today = today,
                    )

                    if (hasCompletedTasks) {
                        item {
                            TaskSectionHeader(
                                title = "Completed",
                                color = onSurfaceVariantColor,
                                count =
                                    state.completedTasks.size +
                                        state.completedSubTasks.values.sumOf { it.size } +
                                        state.orphanCompletedSubTasks.size,
                                isExpanded = sectionExpanded["Completed"] ?: false,
                                onToggle = {
                                    sectionExpanded["Completed"] = !(sectionExpanded["Completed"] ?: false)
                                },
                            )
                        }

                        if (sectionExpanded["Completed"] == true) {
                            items(
                                state.completedTasks,
                                key = { it.id },
                                contentType = { "task_item" },
                            ) { task ->
                                TaskItemWithSubtasks(
                                     task = task,
                                     subtasks = state.completedSubTasks[task.id] ?: emptyList(),
                                     onToggleComplete = onToggleComplete,
                                     onTaskClick = onTaskClick,
                                     isExpanded = state.expandedTaskIds[task.id] ?: true,
                                     onToggleExpanded = {
                                         val nextExpanded = !(state.expandedTaskIds[task.id] ?: true)
                                         onTaskExpandedChange(task.id, nextExpanded)
                                     },
                                     allSubTasks = state.completedSubTasks,
                                     today = today,
                                 )
                            }

                            items(
                                state.orphanCompletedSubTasks,
                                key = { it.id },
                                contentType = { "task_item" },
                            ) { task ->
                                TaskRow(
                                    task = task,
                                    onToggleComplete = { onToggleComplete(task.id, it) },
                                    onClick = { onTaskClick(task.id) },
                                    isSubtask = false,
                                    subtaskCount = 0,
                                    showSubtaskChevron = false,
                                    onToggleExpanded = null,
                                    isExpanded = true,
                                    today = today,
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
    expandedTaskIds: Map<String, Boolean>,
    subTasks: Map<String, List<Task>>,
    onTaskExpandedChange: (String, Boolean) -> Unit,
    onToggleComplete: (String, Boolean) -> Unit,
    onTaskClick: (String) -> Unit,
    today: LocalDate,
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
            },
        )
    }

    if (sectionExpanded[title] != false) {
        items(tasks, key = { it.id }, contentType = { "task_item" }) { task ->
            TaskItemWithSubtasks(
                task = task,
                subtasks = subTasks[task.id] ?: emptyList(),
                onToggleComplete = onToggleComplete,
                onTaskClick = onTaskClick,
                isExpanded = expandedTaskIds[task.id] ?: true,
                onToggleExpanded = {
                    val nextExpanded = !(expandedTaskIds[task.id] ?: true)
                    onTaskExpandedChange(task.id, nextExpanded)
                },
                allSubTasks = subTasks,
                today = today,
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
    onToggle: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(
                    horizontal = Dimensions.screenPaddingHorizontal,
                    vertical = Dimensions.elementSpacing,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
            contentDescription = if (isExpanded) "Collapse $title" else "Expand $title",
            tint = color,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(Dimensions.elementSpacingSmall))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    onToggleExpanded: () -> Unit,
    allSubTasks: Map<String, List<Task>> = emptyMap(),
    depth: Int = 0,
    today: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
) {
    val content: @Composable () -> Unit = {
        Column {
            TaskRow(
                task = task,
                onToggleComplete = { onToggleComplete(task.id, it) },
                onClick = { onTaskClick(task.id) },
                isSubtask = depth > 0,
                subtaskCount = subtasks.size,
                showSubtaskChevron = subtasks.isNotEmpty(),
                onToggleExpanded = if (subtasks.isNotEmpty()) onToggleExpanded else null,
                isExpanded = isExpanded,
                nestingDepth = depth,
                today = today,
            )

            if (subtasks.isNotEmpty() && isExpanded) {
                val nextDepth = depth + 1
                subtasks.forEach { subtask ->
                    val nestedSubtasks = allSubTasks[subtask.id] ?: emptyList()
                    if (nestedSubtasks.isNotEmpty() &&
                        nextDepth < MAX_SUBTASK_DEPTH
                    ) {
                        TaskItemWithSubtasks(
                            task = subtask,
                            subtasks = nestedSubtasks,
                            onToggleComplete = onToggleComplete,
                            onTaskClick = onTaskClick,
                            isExpanded = true,
                            onToggleExpanded = {},
                            allSubTasks = allSubTasks,
                            depth = nextDepth,
                            today = today,
                        )
                    } else {
                        TaskRow(
                            task = subtask,
                            onToggleComplete = { onToggleComplete(subtask.id, it) },
                            onClick = { onTaskClick(subtask.id) },
                            isSubtask = true,
                            subtaskCount = 0,
                            showSubtaskChevron = false,
                            onToggleExpanded = null,
                            isExpanded = true,
                            nestingDepth = nextDepth,
                            today = today,
                        )
                    }
                }
            }
        }
    }

    if (depth == 0) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = Dimensions.cardSpacing,
                        vertical = Dimensions.taskRowVerticalPadding,
                    ),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            shape = RoundedCornerShape(12.dp),
        ) {
            content()
        }
    } else {
        content()
    }
}

/** Task row with priority bar, checkbox, title, metadata, and subtask expand chevron. */
@Composable
private fun TaskRow(
    task: Task,
    onToggleComplete: (Boolean) -> Unit,
    onClick: () -> Unit,
    isSubtask: Boolean,
    subtaskCount: Int,
    showSubtaskChevron: Boolean,
    onToggleExpanded: (() -> Unit)?,
    isExpanded: Boolean,
    nestingDepth: Int = 0,
    today: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
) {
    val priorityColor = getPriorityColor(task.priority)
    val hasPriority = task.priority != TaskPriority.NONE
    val isOverdue = task.dueDate?.let { it < today } == true && !task.isCompleted
    val dimAlpha = Dimensions.COMPLETED_TASK_ALPHA

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(
                    start =
                        Dimensions.screenPaddingHorizontal +
                            Dimensions.subtaskIndentPerLevel * nestingDepth,
                    end = Dimensions.screenPaddingHorizontal,
                    top = Dimensions.taskRowVerticalPadding,
                    bottom = Dimensions.taskRowVerticalPadding,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Priority bar: always rendered (transparent for NONE) to keep checkboxes aligned.
        if (!isSubtask) {
            Box(
                modifier =
                    Modifier
                        .width(3.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(
                            if (task.isCompleted) {
                                priorityColor.copy(alpha = dimAlpha)
                            } else {
                                priorityColor
                            },
                        ),
            )
            Spacer(modifier = Modifier.width(5.dp))
        }

        // Checkbox with priority color — fixed 40dp touch target
        IconButton(
            onClick = { onToggleComplete(!task.isCompleted) },
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                imageVector =
                    if (task.isCompleted) {
                        Icons.Filled.CheckCircle
                    } else {
                        Icons.Outlined.RadioButtonUnchecked
                    },
                contentDescription =
                    if (task.isCompleted) "Completed" else "Incomplete",
                tint =
                    when {
                        task.isCompleted && hasPriority -> priorityColor.copy(alpha = dimAlpha)
                        task.isCompleted -> MaterialTheme.colorScheme.outline.copy(alpha = dimAlpha)
                        hasPriority -> priorityColor
                        else -> MaterialTheme.colorScheme.outline
                    },
                modifier = Modifier.size(22.dp),
            )
        }

        // Content column
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(vertical = Dimensions.elementSpacingLarge),
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                color =
                    if (task.isCompleted) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = dimAlpha)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            // Meta row: description icon, subtask count, due date
            val hasDescription = !task.description.isNullOrBlank()
            val hasDueDate = task.dueDate != null
            val hasSubtasks = subtaskCount > 0
            val metaTint =
                if (task.isCompleted) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = dimAlpha)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }

            if (hasDescription || hasDueDate || hasSubtasks) {
                Spacer(modifier = Modifier.height(Dimensions.elementSpacingSmall))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.elementSpacingSmall),
                ) {
                    if (hasDescription) {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = "Has description",
                            modifier = Modifier.size(14.dp),
                            tint = metaTint,
                        )
                    }

                    if (hasSubtasks && !isSubtask) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.SubdirectoryArrowRight,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = metaTint,
                            )
                            Text(
                                text = "$subtaskCount",
                                style = MaterialTheme.typography.labelSmall,
                                color = metaTint,
                            )
                        }
                    }

                    if (hasDueDate) {
                        Text(
                            text = formatDueDate(task.dueDate!!, today),
                            style = MaterialTheme.typography.labelSmall,
                            color =
                                if (isOverdue) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    metaTint
                                },
                        )
                    }
                }
            }
        }

        // Expand/Collapse chevron for subtasks
        if (showSubtaskChevron && onToggleExpanded != null) {
            IconButton(
                onClick = onToggleExpanded,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                    contentDescription = if (isExpanded) "Collapse subtasks" else "Expand subtasks",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

private fun formatDueDate(
    dueDate: LocalDate,
    today: LocalDate,
): String {
    return when {
        dueDate == today -> "Today"
        dueDate == today.plus(1, DateTimeUnit.DAY) -> "Tomorrow"
        dueDate == today.minus(1, DateTimeUnit.DAY) -> "Yesterday"
        dueDate.year == today.year -> "${dueDate.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} ${dueDate.dayOfMonth}"
        else -> "${dueDate.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} ${dueDate.dayOfMonth}, ${dueDate.year}"
    }
}

private fun getPriorityColor(priority: TaskPriority): Color {
    return when (priority) {
        TaskPriority.HIGH -> Color(0xFFE53935)
        TaskPriority.MEDIUM -> Color(0xFFFFB300)
        TaskPriority.LOW -> Color(0xFF1E88E5)
        TaskPriority.NONE -> Color.Transparent
    }
}

@Composable
private fun TaskOverviewRow(
    totalTasks: Int,
    completedTasks: Int,
    overdueTasks: Int,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = Dimensions.screenPaddingHorizontal,
                    vertical = Dimensions.elementSpacing,
                ),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.elementSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val completionRate = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f
        Text(
            text = "$completedTasks/$totalTasks done",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (overdueTasks > 0) {
            Text(
                text = "· $overdueTasks overdue",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "${(completionRate * 100).toInt()}%",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.TaskAlt,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
        )

        Spacer(modifier = Modifier.height(Dimensions.sectionSpacing))

        Text(
            text = "No tasks yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(Dimensions.elementSpacing))

        Text(
            text = "Use the quick add button to drop a task into Inbox.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
