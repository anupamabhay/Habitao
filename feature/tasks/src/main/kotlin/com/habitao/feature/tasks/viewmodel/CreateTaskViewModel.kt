package com.habitao.feature.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habitao.domain.model.Task
import com.habitao.domain.model.TaskPriority
import com.habitao.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject

data class SubtaskItem(
    val id: String,
    val text: String,
    val priority: TaskPriority = TaskPriority.NONE
)

data class CreateTaskState(
    val title: String = "",
    val description: String = "",
    val dueDate: LocalDate? = null,
    val dueTime: LocalTime? = null,
    val priority: TaskPriority = TaskPriority.NONE,
    val reminderEnabled: Boolean = false,
    val reminderMinutesBefore: Int = 30,
    val subtasks: List<SubtaskItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isEditMode: Boolean = false,
    val isSaving: Boolean = false
)

sealed class CreateTaskIntent {
    data class SetTitle(val title: String) : CreateTaskIntent()
    data class SetDescription(val description: String) : CreateTaskIntent()
    data class SetDueDate(val date: LocalDate?) : CreateTaskIntent()
    data class SetDueTime(val time: LocalTime?) : CreateTaskIntent()
    data class SetPriority(val priority: TaskPriority) : CreateTaskIntent()
    data class SetReminderEnabled(val enabled: Boolean) : CreateTaskIntent()
    data class SetReminderMinutesBefore(val minutes: Int) : CreateTaskIntent()
    object AddSubtask : CreateTaskIntent()
    data class RemoveSubtask(val id: String) : CreateTaskIntent()
    data class UpdateSubtaskText(val id: String, val text: String) : CreateTaskIntent()
    data class UpdateSubtaskPriority(val id: String, val priority: TaskPriority) : CreateTaskIntent()
    object SaveTask : CreateTaskIntent()
    data class LoadTask(val taskId: String) : CreateTaskIntent()
    object ClearError : CreateTaskIntent()
    object ResetForm : CreateTaskIntent()
}

@HiltViewModel
class CreateTaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CreateTaskState())
    val state: StateFlow<CreateTaskState> = _state.asStateFlow()

    private val _savedEvent = MutableSharedFlow<Unit>()
    val savedEvent: SharedFlow<Unit> = _savedEvent.asSharedFlow()

    private var currentTaskId: String? = null

    fun processIntent(intent: CreateTaskIntent) {
        when (intent) {
            is CreateTaskIntent.SetTitle -> {
                _state.update { it.copy(title = intent.title, error = null) }
            }
            is CreateTaskIntent.SetDescription -> {
                _state.update { it.copy(description = intent.description) }
            }
            is CreateTaskIntent.SetDueDate -> {
                _state.update { it.copy(dueDate = intent.date) }
            }
            is CreateTaskIntent.SetDueTime -> {
                _state.update { it.copy(dueTime = intent.time) }
            }
            is CreateTaskIntent.SetPriority -> {
                _state.update { it.copy(priority = intent.priority) }
            }
            is CreateTaskIntent.SetReminderEnabled -> {
                _state.update { it.copy(reminderEnabled = intent.enabled) }
            }
            is CreateTaskIntent.SetReminderMinutesBefore -> {
                _state.update { it.copy(reminderMinutesBefore = intent.minutes) }
            }
            is CreateTaskIntent.AddSubtask -> {
                _state.update { 
                    it.copy(
                        subtasks = it.subtasks + SubtaskItem(
                            id = UUID.randomUUID().toString(),
                            text = "",
                            priority = TaskPriority.NONE
                        )
                    ) 
                }
            }
            is CreateTaskIntent.RemoveSubtask -> {
                _state.update { 
                    it.copy(
                        subtasks = it.subtasks.filter { subtask -> subtask.id != intent.id }
                    ) 
                }
            }
            is CreateTaskIntent.UpdateSubtaskText -> {
                _state.update { 
                    it.copy(
                        subtasks = it.subtasks.map { subtask ->
                            if (subtask.id == intent.id) subtask.copy(text = intent.text) else subtask
                        }
                    ) 
                }
            }
            is CreateTaskIntent.UpdateSubtaskPriority -> {
                _state.update {
                    it.copy(
                        subtasks = it.subtasks.map { subtask ->
                            if (subtask.id == intent.id) {
                                subtask.copy(priority = intent.priority)
                            } else {
                                subtask
                            }
                        }
                    )
                }
            }
            is CreateTaskIntent.SaveTask -> saveTask()
            is CreateTaskIntent.LoadTask -> loadTask(intent.taskId)
            is CreateTaskIntent.ClearError -> {
                _state.update { it.copy(error = null) }
            }
            is CreateTaskIntent.ResetForm -> {
                currentTaskId = null
                _state.value = CreateTaskState()
            }
        }
    }

    private fun loadTask(taskId: String) {
        currentTaskId = taskId
        _state.update { it.copy(isLoading = true, isEditMode = true) }
        viewModelScope.launch {
            taskRepository.getTaskById(taskId).fold(
                onSuccess = { task ->
                    _state.update {
                        it.copy(
                            title = task.title,
                            description = task.description ?: "",
                            dueDate = task.dueDate,
                            dueTime = task.dueTime,
                            priority = task.priority,
                            reminderEnabled = task.reminderEnabled,
                            reminderMinutesBefore = task.reminderMinutesBefore,
                            isLoading = false
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load task"
                        )
                    }
                }
            )
        }
    }

    private fun saveTask() {
        val currentState = _state.value
        if (currentState.title.isBlank()) {
            _state.update { it.copy(error = "Title cannot be empty") }
            return
        }

        _state.update { it.copy(isSaving = true, error = null) }

        viewModelScope.launch {
            val taskId = currentTaskId ?: UUID.randomUUID().toString()
            
            val task = Task(
                id = taskId,
                title = currentState.title.trim(),
                description = currentState.description.trim().takeIf { it.isNotEmpty() },
                dueDate = currentState.dueDate,
                dueTime = currentState.dueTime,
                priority = currentState.priority,
                reminderEnabled = currentState.reminderEnabled,
                reminderMinutesBefore = currentState.reminderMinutesBefore
            )

            val result = if (currentTaskId != null) {
                taskRepository.updateTask(task)
            } else {
                taskRepository.createTask(task)
            }

            result.fold(
                onSuccess = {
                    val validSubtasks = currentState.subtasks.filter { it.text.isNotBlank() }
                    var allSubtasksSaved = true
                    
                    for (subtask in validSubtasks) {
                        val subtaskModel = Task(
                            id = UUID.randomUUID().toString(),
                            title = subtask.text.trim(),
                            parentTaskId = taskId,
                            dueDate = currentState.dueDate,
                            dueTime = currentState.dueTime,
                            priority = subtask.priority
                        )
                        val subtaskResult = taskRepository.createTask(subtaskModel)
                        if (subtaskResult.isFailure) {
                            allSubtasksSaved = false
                            break
                        }
                    }

                    if (allSubtasksSaved) {
                        _state.update { it.copy(isSaving = false) }
                        _savedEvent.emit(Unit)
                    } else {
                        _state.update { 
                            it.copy(
                                isSaving = false, 
                                error = "Failed to save some subtasks"
                            ) 
                        }
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isSaving = false,
                            error = error.message ?: "Failed to save task"
                        )
                    }
                }
            )
        }
    }
}
