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
    val priority: TaskPriority = TaskPriority.NONE,
    val existingTaskId: String? = null,
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
    val isSaving: Boolean = false,
    val parentTaskId: String? = null,
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
class CreateTaskViewModel
    @Inject
    constructor(
        private val taskRepository: TaskRepository,
    ) : ViewModel() {
        private val _state = MutableStateFlow(CreateTaskState())
        val state: StateFlow<CreateTaskState> = _state.asStateFlow()

        private val _savedEvent = MutableSharedFlow<Unit>()
        val savedEvent: SharedFlow<Unit> = _savedEvent.asSharedFlow()

        private var currentTaskId: String? = null
        private var loadedTask: Task? = null

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
                            subtasks =
                                it.subtasks +
                                    SubtaskItem(
                                        id = UUID.randomUUID().toString(),
                                        text = "",
                                        priority = TaskPriority.NONE,
                                        existingTaskId = null,
                                    ),
                        )
                    }
                }
                is CreateTaskIntent.RemoveSubtask -> {
                    _state.update {
                        it.copy(
                            subtasks = it.subtasks.filter { subtask -> subtask.id != intent.id },
                        )
                    }
                }
                is CreateTaskIntent.UpdateSubtaskText -> {
                    _state.update {
                        it.copy(
                            subtasks =
                                it.subtasks.map { subtask ->
                                    if (subtask.id == intent.id) subtask.copy(text = intent.text) else subtask
                                },
                        )
                    }
                }
                is CreateTaskIntent.UpdateSubtaskPriority -> {
                    _state.update {
                        it.copy(
                            subtasks =
                                it.subtasks.map { subtask ->
                                    if (subtask.id == intent.id) {
                                        subtask.copy(priority = intent.priority)
                                    } else {
                                        subtask
                                    }
                                },
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
                    loadedTask = null
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
                        loadedTask = task
                        val subtaskItems = loadSubtasks(taskId)
                        _state.update {
                            it.copy(
                                title = task.title,
                                description = task.description ?: "",
                                dueDate = task.dueDate,
                                dueTime = task.dueTime,
                                priority = task.priority,
                                reminderEnabled = task.reminderEnabled,
                                reminderMinutesBefore = task.reminderMinutesBefore,
                                parentTaskId = task.parentTaskId,
                                subtasks = subtaskItems,
                                isLoading = false,
                            )
                        }
                    },
                    onFailure = { error ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = error.message ?: "Failed to load task",
                            )
                        }
                    },
                )
            }
        }

        private suspend fun loadSubtasks(parentId: String): List<SubtaskItem> {
            return taskRepository.getSubtasksByParentId(parentId).fold(
                onSuccess = { tasks ->
                    tasks.map { task ->
                        SubtaskItem(
                            id = task.id,
                            text = task.title,
                            priority = task.priority,
                            existingTaskId = task.id,
                        )
                    }
                },
                onFailure = { emptyList() },
            )
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
                val existing = loadedTask

                val task =
                    Task(
                        id = taskId,
                        title = currentState.title.trim(),
                        description = currentState.description.trim().takeIf { it.isNotEmpty() },
                        parentTaskId = currentState.parentTaskId,
                        dueDate = currentState.dueDate,
                        dueTime = currentState.dueTime,
                        priority = currentState.priority,
                        reminderEnabled = currentState.reminderEnabled,
                        reminderMinutesBefore = currentState.reminderMinutesBefore,
                        isCompleted = existing?.isCompleted ?: false,
                        completedAt = existing?.completedAt,
                        createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        tags = existing?.tags ?: emptyList(),
                        sortOrder = existing?.sortOrder ?: 0,
                        syncStatus = existing?.syncStatus ?: com.habitao.domain.model.SyncStatus.LOCAL,
                    )

                val result =
                    if (currentTaskId != null) {
                        taskRepository.updateTask(task)
                    } else {
                        taskRepository.createTask(task)
                    }

                result.fold(
                    onSuccess = {
                        if (currentState.parentTaskId == null) {
                            try {
                                saveSubtasks(taskId, currentState)
                                _state.update { it.copy(isSaving = false) }
                                _savedEvent.emit(Unit)
                            } catch (e: Exception) {
                                _state.update {
                                    it.copy(
                                        isSaving = false,
                                        error = e.message ?: "Failed to save subtasks",
                                    )
                                }
                            }
                        } else {
                            _state.update { it.copy(isSaving = false) }
                            _savedEvent.emit(Unit)
                        }
                    },
                    onFailure = { error ->
                        _state.update {
                            it.copy(
                                isSaving = false,
                                error = error.message ?: "Failed to save task",
                            )
                        }
                    },
                )
            }
        }

        private suspend fun saveSubtasks(
            parentId: String,
            currentState: CreateTaskState,
        ) {
            val uiSubtasks = currentState.subtasks.filter { it.text.isNotBlank() }
            val existingSubtaskIds = uiSubtasks.mapNotNull { it.existingTaskId }.toSet()

            val dbSubtasksResult = taskRepository.getSubtasksByParentId(parentId)
            if (dbSubtasksResult.isFailure) {
                throw Exception("Failed to load existing subtasks")
            }
            val dbSubtasks = dbSubtasksResult.getOrNull() ?: emptyList()
            val dbSubtasksMap = dbSubtasks.associateBy { it.id }

            // Delete subtasks removed from UI
            for (dbSubtask in dbSubtasks) {
                if (dbSubtask.id !in existingSubtaskIds) {
                    val deleteResult = taskRepository.deleteTask(dbSubtask.id)
                    if (deleteResult.isFailure) throw Exception("Failed to delete subtask")
                }
            }

            // Update existing or create new subtasks
            for (subtask in uiSubtasks) {
                if (subtask.existingTaskId != null) {
                    val existingTask = dbSubtasksMap[subtask.existingTaskId]
                    if (existingTask != null) {
                        val updated =
                            existingTask.copy(
                                title = subtask.text.trim(),
                                priority = subtask.priority,
                                parentTaskId = parentId,
                                updatedAt = System.currentTimeMillis(),
                            )
                        val updateResult = taskRepository.updateTask(updated)
                        if (updateResult.isFailure) throw Exception("Failed to update subtask")
                    }
                } else {
                    val newSubtask =
                        Task(
                            id = UUID.randomUUID().toString(),
                            title = subtask.text.trim(),
                            parentTaskId = parentId,
                            dueDate = currentState.dueDate,
                            dueTime = currentState.dueTime,
                            priority = subtask.priority,
                        )
                    val createResult = taskRepository.createTask(newSubtask)
                    if (createResult.isFailure) throw Exception("Failed to create subtask")
                }
            }
        }
    }
