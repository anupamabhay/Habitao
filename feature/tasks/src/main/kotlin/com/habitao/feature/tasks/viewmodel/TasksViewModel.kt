package com.habitao.feature.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habitao.domain.model.Task
import com.habitao.domain.model.TaskPriority
import com.habitao.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

enum class TaskFilter {
    ALL,
    COMPLETED,
    INCOMPLETE,
}

enum class TaskSortOrder {
    DATE,
    PRIORITY,
    ALPHABETICAL,
}

data class TasksState(
    val overdueTasks: List<Task> = emptyList(),
    val todayTasks: List<Task> = emptyList(),
    val tomorrowTasks: List<Task> = emptyList(),
    val upcomingTasks: List<Task> = emptyList(),
    val subTasks: Map<String, List<Task>> = emptyMap(),
    val completedTasks: List<Task> = emptyList(),
    val completedSubTasks: Map<String, List<Task>> = emptyMap(),
    val orphanCompletedSubTasks: List<Task> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val filter: TaskFilter = TaskFilter.ALL,
    val sortOrder: TaskSortOrder = TaskSortOrder.DATE,
)

sealed class TasksIntent {
    data class CreateTask(val task: Task) : TasksIntent()

    data class UpdateTask(val task: Task) : TasksIntent()

    data class DeleteTask(val taskId: String) : TasksIntent()

    data class ToggleComplete(val taskId: String, val isCompleted: Boolean) : TasksIntent()

    data class SetFilter(val filter: TaskFilter) : TasksIntent()

    data class SetSortOrder(val sortOrder: TaskSortOrder) : TasksIntent()
}

@HiltViewModel
class TasksViewModel
    @Inject
    constructor(
        private val taskRepository: TaskRepository,
    ) : ViewModel() {
        private val errorFlow = MutableStateFlow<String?>(null)
        private val filterFlow = MutableStateFlow(TaskFilter.ALL)
        private val sortOrderFlow = MutableStateFlow(TaskSortOrder.DATE)

        val state: StateFlow<TasksState> =
            combine(
                taskRepository.observeAllTasks()
                    .map { result -> result.getOrElse { emptyList() } }
                    .catch { emit(emptyList()) },
                errorFlow,
                filterFlow,
                sortOrderFlow,
            ) { tasks, error, filter, sortOrder ->
                val filteredTasks =
                    when (filter) {
                        TaskFilter.ALL -> tasks
                        TaskFilter.COMPLETED -> tasks.filter { it.isCompleted }
                        TaskFilter.INCOMPLETE -> tasks.filter { !it.isCompleted }
                    }

                val allTopLevelTasks = filteredTasks.filter { it.parentTaskId == null }
                val allSubTasksByParent = filteredTasks.filter { it.parentTaskId != null }.groupBy { it.parentTaskId!! }

                // A parent task is fully completed ONLY if it is marked completed AND all its subtasks are marked completed
                val fullyCompletedParentIds =
                    allTopLevelTasks.filter { parent ->
                        parent.isCompleted && (allSubTasksByParent[parent.id]?.all { it.isCompleted } ?: true)
                    }.map { it.id }.toSet()

                val activeTopLevelTasks = allTopLevelTasks.filterNot { it.id in fullyCompletedParentIds }
                val completedTopLevelTasks = allTopLevelTasks.filter { it.id in fullyCompletedParentIds }

                // Subtasks of active parents stay with the active parent (even if the subtask itself is completed)
                // Build complete subtask map including nested sub-subtasks
                val activeSubTasks =
                    buildMap<String, List<Task>> {
                        fun addDescendants(parentId: String) {
                            val children = allSubTasksByParent[parentId] ?: return
                            put(parentId, sortTasks(children, sortOrder))
                            children.forEach { child -> addDescendants(child.id) }
                        }
                        activeTopLevelTasks.forEach { task -> addDescendants(task.id) }
                    }

                val completedSubTasks =
                    buildMap<String, List<Task>> {
                        fun addDescendants(parentId: String) {
                            val children = allSubTasksByParent[parentId] ?: return
                            put(parentId, sortTasks(children, sortOrder))
                            children.forEach { child -> addDescendants(child.id) }
                        }
                        completedTopLevelTasks.forEach { task -> addDescendants(task.id) }
                    }

                val knownParentIds = allTopLevelTasks.map { it.id }.toSet()
                val orphanCompletedSubTasks =
                    allSubTasksByParent
                        .filterKeys { it !in knownParentIds }
                        .values
                        .flatten()
                        .filter { it.isCompleted }

                val today = LocalDate.now()
                val tomorrow = today.plusDays(1)

                val overdueTasks = mutableListOf<Task>()
                val todayTasks = mutableListOf<Task>()
                val tomorrowTasks = mutableListOf<Task>()
                val upcomingTasks = mutableListOf<Task>()

                activeTopLevelTasks.forEach { task ->
                    val dueDate = task.dueDate
                    if (dueDate == null) {
                        upcomingTasks.add(task)
                    } else if (dueDate.isBefore(today)) {
                        overdueTasks.add(task)
                    } else if (dueDate.isEqual(today)) {
                        todayTasks.add(task)
                    } else if (dueDate.isEqual(tomorrow)) {
                        tomorrowTasks.add(task)
                    } else {
                        upcomingTasks.add(task)
                    }
                }

                TasksState(
                    overdueTasks = sortTasks(overdueTasks, sortOrder),
                    todayTasks = sortTasks(todayTasks, sortOrder),
                    tomorrowTasks = sortTasks(tomorrowTasks, sortOrder),
                    upcomingTasks = sortTasks(upcomingTasks, sortOrder),
                    subTasks = activeSubTasks,
                    completedTasks = sortTasks(completedTopLevelTasks, sortOrder),
                    completedSubTasks = completedSubTasks,
                    orphanCompletedSubTasks = sortTasks(orphanCompletedSubTasks, sortOrder),
                    isLoading = false,
                    error = error,
                    filter = filter,
                    sortOrder = sortOrder,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = TasksState(),
            )

        fun processIntent(intent: TasksIntent) {
            when (intent) {
                is TasksIntent.CreateTask -> createTask(intent.task)
                is TasksIntent.UpdateTask -> updateTask(intent.task)
                is TasksIntent.DeleteTask -> deleteTask(intent.taskId)
                is TasksIntent.ToggleComplete -> toggleComplete(intent.taskId, intent.isCompleted)
                is TasksIntent.SetFilter -> setFilter(intent.filter)
                is TasksIntent.SetSortOrder -> setSortOrder(intent.sortOrder)
            }
        }

        private fun createTask(task: Task) {
            viewModelScope.launch {
                val saveResult =
                    taskRepository.getTaskById(task.id).fold(
                        onSuccess = { existingTask ->
                            taskRepository.upsertTask(
                                task.copy(
                                    createdAt = existingTask.createdAt,
                                    updatedAt = System.currentTimeMillis(),
                                ),
                            )
                        },
                        onFailure = {
                            taskRepository.upsertTask(task)
                        },
                    )

                saveResult.onFailure {
                    errorFlow.value = it.message ?: "Failed to save task"
                }
            }
        }

        private fun updateTask(task: Task) {
            viewModelScope.launch {
                taskRepository.updateTask(task).onFailure {
                    errorFlow.value = it.message ?: "Failed to update task"
                }
            }
        }

        private fun deleteTask(taskId: String) {
            viewModelScope.launch {
                taskRepository.deleteTask(taskId).onFailure {
                    errorFlow.value = it.message ?: "Failed to delete task"
                }
            }
        }

        private fun toggleComplete(
            taskId: String,
            isCompleted: Boolean,
        ) {
            viewModelScope.launch {
                taskRepository.getTaskById(taskId).onSuccess { task ->
                    val updatedTask =
                        task.copy(
                            isCompleted = isCompleted,
                            completedAt = if (isCompleted) System.currentTimeMillis() else null,
                        )
                    taskRepository.updateTask(updatedTask).onFailure {
                        errorFlow.value = it.message ?: "Failed to update task"
                    }
                }.onFailure {
                    errorFlow.value = it.message ?: "Failed to find task"
                }
            }
        }

        private fun setFilter(filter: TaskFilter) {
            filterFlow.value = filter
        }

        private fun setSortOrder(sortOrder: TaskSortOrder) {
            sortOrderFlow.value = sortOrder
        }

        private fun sortTasks(
            tasks: List<Task>,
            sortOrder: TaskSortOrder,
        ): List<Task> {
            val dateComparator =
                compareBy<Task> { it.dueDate ?: LocalDate.MAX }
                    .thenBy { it.dueTime ?: LocalTime.MAX }
            val priorityComparator = compareBy<Task> { priorityRank(it.priority) }
            val titleComparator = compareBy<Task> { it.title.trim().lowercase() }

            val comparator =
                when (sortOrder) {
                    TaskSortOrder.DATE ->
                        dateComparator
                            .thenBy { priorityRank(it.priority) }
                            .thenBy { it.title.trim().lowercase() }

                    TaskSortOrder.PRIORITY ->
                        priorityComparator
                            .thenBy { it.dueDate ?: LocalDate.MAX }
                            .thenBy { it.dueTime ?: LocalTime.MAX }
                            .thenBy { it.title.trim().lowercase() }

                    TaskSortOrder.ALPHABETICAL ->
                        titleComparator
                            .thenBy { it.dueDate ?: LocalDate.MAX }
                            .thenBy { priorityRank(it.priority) }
                }

            return tasks.sortedWith(comparator)
        }

        private fun priorityRank(priority: TaskPriority): Int {
            return when (priority) {
                TaskPriority.HIGH -> 0
                TaskPriority.MEDIUM -> 1
                TaskPriority.LOW -> 2
                TaskPriority.NONE -> 3
            }
        }

        fun clearError() {
            errorFlow.value = null
        }
    }
