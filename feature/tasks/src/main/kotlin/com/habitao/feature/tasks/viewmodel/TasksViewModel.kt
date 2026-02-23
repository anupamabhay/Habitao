package com.habitao.feature.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habitao.domain.model.Task
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
import javax.inject.Inject

enum class TaskFilter {
    ALL,
    COMPLETED,
    INCOMPLETE
}

data class TasksState(
    val overdueTasks: List<Task> = emptyList(),
    val todayTasks: List<Task> = emptyList(),
    val tomorrowTasks: List<Task> = emptyList(),
    val upcomingTasks: List<Task> = emptyList(),
    val subTasks: Map<String, List<Task>> = emptyMap(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val filter: TaskFilter = TaskFilter.INCOMPLETE
)

sealed class TasksIntent {
    data class CreateTask(val task: Task) : TasksIntent()
    data class UpdateTask(val task: Task) : TasksIntent()
    data class DeleteTask(val taskId: String) : TasksIntent()
    data class ToggleComplete(val taskId: String, val isCompleted: Boolean) : TasksIntent()
    data class SetFilter(val filter: TaskFilter) : TasksIntent()
}

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val errorFlow = MutableStateFlow<String?>(null)
    private val filterFlow = MutableStateFlow(TaskFilter.INCOMPLETE)

    val state: StateFlow<TasksState> = combine(
        taskRepository.observeAllTasks()
            .map { result -> result.getOrElse { emptyList() } }
            .catch { emit(emptyList()) },
        errorFlow,
        filterFlow
    ) { tasks, error, filter ->
        val filteredTasks = when (filter) {
            TaskFilter.ALL -> tasks
            TaskFilter.COMPLETED -> tasks.filter { it.isCompleted }
            TaskFilter.INCOMPLETE -> tasks.filter { !it.isCompleted }
        }

        val topLevelTasks = filteredTasks.filter { it.parentTaskId == null }
        val subTasks = filteredTasks.filter { it.parentTaskId != null }.groupBy { it.parentTaskId!! }

        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)

        val overdueTasks = mutableListOf<Task>()
        val todayTasks = mutableListOf<Task>()
        val tomorrowTasks = mutableListOf<Task>()
        val upcomingTasks = mutableListOf<Task>()

        topLevelTasks.forEach { task ->
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
            overdueTasks = overdueTasks.sortedBy { it.dueDate },
            todayTasks = todayTasks.sortedBy { it.dueDate },
            tomorrowTasks = tomorrowTasks.sortedBy { it.dueDate },
            upcomingTasks = upcomingTasks.sortedBy { it.dueDate },
            subTasks = subTasks,
            isLoading = false,
            error = error,
            filter = filter
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TasksState()
    )

    fun processIntent(intent: TasksIntent) {
        when (intent) {
            is TasksIntent.CreateTask -> createTask(intent.task)
            is TasksIntent.UpdateTask -> updateTask(intent.task)
            is TasksIntent.DeleteTask -> deleteTask(intent.taskId)
            is TasksIntent.ToggleComplete -> toggleComplete(intent.taskId, intent.isCompleted)
            is TasksIntent.SetFilter -> setFilter(intent.filter)
        }
    }

    private fun createTask(task: Task) {
        viewModelScope.launch {
            taskRepository.createTask(task).onFailure {
                errorFlow.value = it.message ?: "Failed to create task"
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

    private fun toggleComplete(taskId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            taskRepository.getTaskById(taskId).onSuccess { task ->
                val updatedTask = task.copy(
                    isCompleted = isCompleted,
                    completedAt = if (isCompleted) System.currentTimeMillis() else null
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

    fun clearError() {
        errorFlow.value = null
    }
}
