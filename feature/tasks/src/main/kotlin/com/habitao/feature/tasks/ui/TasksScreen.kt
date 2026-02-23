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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.habitao.core.ui.theme.Dimensions
import com.habitao.domain.model.Task
import com.habitao.domain.model.TaskPriority
import com.habitao.feature.tasks.viewmodel.TasksIntent
import com.habitao.feature.tasks.viewmodel.TasksState
import com.habitao.feature.tasks.viewmodel.TasksViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    onAddTask: () -> Unit,
    onEditTask: (String) -> Unit = {},
    viewModel: TasksViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Tasks") },
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
    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            state.overdueTasks.isEmpty() && state.todayTasks.isEmpty() && 
            state.tomorrowTasks.isEmpty() && state.upcomingTasks.isEmpty() -> {
                EmptyState(modifier = Modifier.align(Alignment.Center))
            }
            else -> {
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
                    if (state.overdueTasks.isNotEmpty()) {
                        item { TaskSectionHeader("Overdue", MaterialTheme.colorScheme.error) }
                        items(state.overdueTasks, key = { it.id }) { task ->
                            TaskItemWithSubtasks(
                                task = task,
                                subtasks = state.subTasks[task.id] ?: emptyList(),
                                onToggleComplete = onToggleComplete,
                                onTaskClick = onTaskClick
                            )
                        }
                    }

                    if (state.todayTasks.isNotEmpty()) {
                        item { TaskSectionHeader("Today", MaterialTheme.colorScheme.primary) }
                        items(state.todayTasks, key = { it.id }) { task ->
                            TaskItemWithSubtasks(
                                task = task,
                                subtasks = state.subTasks[task.id] ?: emptyList(),
                                onToggleComplete = onToggleComplete,
                                onTaskClick = onTaskClick
                            )
                        }
                    }

                    if (state.tomorrowTasks.isNotEmpty()) {
                        item { TaskSectionHeader("Tomorrow", MaterialTheme.colorScheme.secondary) }
                        items(state.tomorrowTasks, key = { it.id }) { task ->
                            TaskItemWithSubtasks(
                                task = task,
                                subtasks = state.subTasks[task.id] ?: emptyList(),
                                onToggleComplete = onToggleComplete,
                                onTaskClick = onTaskClick
                            )
                        }
                    }

                    if (state.upcomingTasks.isNotEmpty()) {
                        item { TaskSectionHeader("Upcoming", MaterialTheme.colorScheme.onSurfaceVariant) }
                        items(state.upcomingTasks, key = { it.id }) { task ->
                            TaskItemWithSubtasks(
                                task = task,
                                subtasks = state.subTasks[task.id] ?: emptyList(),
                                onToggleComplete = onToggleComplete,
                                onTaskClick = onTaskClick
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskSectionHeader(title: String, color: Color) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = color,
        modifier = Modifier.padding(vertical = Dimensions.elementSpacingSmall, horizontal = Dimensions.elementSpacingSmall)
    )
}

@Composable
private fun TaskItemWithSubtasks(
    task: Task,
    subtasks: List<Task>,
    onToggleComplete: (String, Boolean) -> Unit,
    onTaskClick: (String) -> Unit
) {
    Column {
        TaskRow(
            task = task,
            onToggleComplete = { onToggleComplete(task.id, it) },
            onClick = { onTaskClick(task.id) }
        )
        
        if (subtasks.isNotEmpty()) {
            Column(
                modifier = Modifier.padding(start = 32.dp)
            ) {
                subtasks.forEach { subtask ->
                    TaskRow(
                        task = subtask,
                        onToggleComplete = { onToggleComplete(subtask.id, it) },
                        onClick = { onTaskClick(subtask.id) }
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
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Dimensions.elementSpacingSmall, horizontal = Dimensions.elementSpacingSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = onToggleComplete
        )
        
        Spacer(modifier = Modifier.width(Dimensions.elementSpacingSmall))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                
                if (task.priority != TaskPriority.NONE) {
                    Spacer(modifier = Modifier.width(Dimensions.elementSpacingSmall))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(getPriorityColor(task.priority))
                    )
                }
            }
            
            task.dueDate?.let { date ->
                Text(
                    text = date.format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun getPriorityColor(priority: TaskPriority): Color {
    return when (priority) {
        TaskPriority.HIGH -> Color(0xFFE53935) // Red
        TaskPriority.MEDIUM -> Color(0xFFFFB300) // Amber
        TaskPriority.LOW -> Color(0xFF1E88E5) // Blue
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
