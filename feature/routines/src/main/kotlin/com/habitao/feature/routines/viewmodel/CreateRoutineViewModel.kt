package com.habitao.feature.routines.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habitao.domain.model.RepeatPattern
import com.habitao.domain.model.Routine
import com.habitao.domain.model.RoutineStep
import com.habitao.domain.repository.RoutineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject

data class RoutineStepItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val estimatedMinutes: Int? = null,
)

data class CreateRoutineState(
    val title: String = "",
    val description: String = "",
    val steps: List<RoutineStepItem> = emptyList(),
    val repeatPattern: RepeatPattern = RepeatPattern.DAILY,
    val scheduledDays: Set<DayOfWeek> = emptySet(),
    val reminderEnabled: Boolean = false,
    val reminderTime: LocalTime? = null,
    val customInterval: Int = 2,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val isEditMode: Boolean = false,
)

sealed class CreateRoutineIntent {
    data class SetTitle(val title: String) : CreateRoutineIntent()

    data class SetDescription(val description: String) : CreateRoutineIntent()

    object AddStep : CreateRoutineIntent()

    data class RemoveStep(val index: Int) : CreateRoutineIntent()

    data class UpdateStepTitle(val index: Int, val title: String) : CreateRoutineIntent()

    data class UpdateStepDuration(val index: Int, val duration: Int?) : CreateRoutineIntent()

    data class ReorderSteps(val fromIndex: Int, val toIndex: Int) : CreateRoutineIntent()

    data class SetRepeatPattern(val pattern: RepeatPattern) : CreateRoutineIntent()

    data class ToggleDay(val day: DayOfWeek) : CreateRoutineIntent()

    data class SetReminderEnabled(val enabled: Boolean) : CreateRoutineIntent()

    data class SetReminderTime(val time: LocalTime) : CreateRoutineIntent()

    data class SetCustomInterval(val interval: Int) : CreateRoutineIntent()

    data class LoadRoutine(val routineId: String) : CreateRoutineIntent()

    object ResetForm : CreateRoutineIntent()

    object SaveRoutine : CreateRoutineIntent()

    object ClearError : CreateRoutineIntent()
}

@HiltViewModel
class CreateRoutineViewModel
    @Inject
    constructor(
        private val routineRepository: RoutineRepository,
    ) : ViewModel() {
        private val _state = MutableStateFlow(CreateRoutineState())
        val state: StateFlow<CreateRoutineState> = _state.asStateFlow()

        private val _savedEvent = MutableSharedFlow<Unit>()
        val savedEvent: SharedFlow<Unit> = _savedEvent.asSharedFlow()

        private var currentRoutineId: String? = null
        private var existingRoutine: Routine? = null
        private var existingStepsById: Map<String, RoutineStep> = emptyMap()

        fun processIntent(intent: CreateRoutineIntent) {
            when (intent) {
                is CreateRoutineIntent.SetTitle -> {
                    _state.update { it.copy(title = intent.title, error = null) }
                }
                is CreateRoutineIntent.SetDescription -> {
                    _state.update { it.copy(description = intent.description) }
                }
                is CreateRoutineIntent.AddStep -> {
                    _state.update { it.copy(steps = it.steps + RoutineStepItem()) }
                }
                is CreateRoutineIntent.RemoveStep -> {
                    _state.update {
                        val newSteps = it.steps.toMutableList().apply { removeAt(intent.index) }
                        it.copy(steps = newSteps)
                    }
                }
                is CreateRoutineIntent.UpdateStepTitle -> {
                    _state.update {
                        val newSteps = it.steps.toMutableList()
                        newSteps[intent.index] = newSteps[intent.index].copy(title = intent.title)
                        it.copy(steps = newSteps)
                    }
                }
                is CreateRoutineIntent.UpdateStepDuration -> {
                    _state.update {
                        val newSteps = it.steps.toMutableList()
                        newSteps[intent.index] = newSteps[intent.index].copy(estimatedMinutes = intent.duration)
                        it.copy(steps = newSteps)
                    }
                }
                is CreateRoutineIntent.ReorderSteps -> {
                    _state.update {
                        val newSteps = it.steps.toMutableList()
                        val item = newSteps.removeAt(intent.fromIndex)
                        newSteps.add(intent.toIndex, item)
                        it.copy(steps = newSteps)
                    }
                }
                is CreateRoutineIntent.SetRepeatPattern -> {
                    _state.update { it.copy(repeatPattern = intent.pattern) }
                }
                is CreateRoutineIntent.ToggleDay -> {
                    _state.update {
                        val newDays =
                            if (it.scheduledDays.contains(intent.day)) {
                                it.scheduledDays - intent.day
                            } else {
                                it.scheduledDays + intent.day
                            }
                        it.copy(scheduledDays = newDays)
                    }
                }
                is CreateRoutineIntent.SetReminderEnabled -> {
                    _state.update { it.copy(reminderEnabled = intent.enabled) }
                }
                is CreateRoutineIntent.SetReminderTime -> {
                    _state.update { it.copy(reminderTime = intent.time) }
                }
                is CreateRoutineIntent.SetCustomInterval -> {
                    _state.update { it.copy(customInterval = intent.interval) }
                }
                is CreateRoutineIntent.LoadRoutine -> loadRoutine(intent.routineId)
                is CreateRoutineIntent.ResetForm -> resetForm()
                is CreateRoutineIntent.SaveRoutine -> saveRoutine()
                is CreateRoutineIntent.ClearError -> {
                    _state.update { it.copy(error = null) }
                }
            }
        }

        private fun resetForm() {
            currentRoutineId = null
            existingRoutine = null
            existingStepsById = emptyMap()
            _state.value = CreateRoutineState()
        }

        private fun loadRoutine(routineId: String) {
            currentRoutineId = routineId
            existingRoutine = null
            existingStepsById = emptyMap()
            _state.value = CreateRoutineState(isLoading = true, isEditMode = true)

            viewModelScope.launch {
                routineRepository.getRoutineById(routineId).fold(
                    onSuccess = { routine ->
                        routineRepository.getRoutineSteps(routineId).fold(
                            onSuccess = { steps ->
                                existingRoutine = routine
                                existingStepsById = steps.associateBy { it.id }

                                _state.update {
                                    it.copy(
                                        title = routine.title,
                                        description = routine.description.orEmpty(),
                                        steps =
                                            steps.sortedBy { step -> step.stepOrder }.map { step ->
                                                RoutineStepItem(
                                                    id = step.id,
                                                    title = step.title,
                                                    estimatedMinutes = step.estimatedDurationMinutes,
                                                )
                                            },
                                        repeatPattern = routine.repeatPattern,
                                        scheduledDays = routine.repeatDays?.toSet() ?: emptySet(),
                                        reminderEnabled = routine.reminderEnabled,
                                        reminderTime = routine.reminderTime,
                                        customInterval = routine.customInterval ?: 2,
                                        isLoading = false,
                                        error = null,
                                        isEditMode = true,
                                    )
                                }
                            },
                            onFailure = { error ->
                                _state.update {
                                    it.copy(
                                        isLoading = false,
                                        error = error.message ?: "Failed to load routine steps",
                                        isEditMode = true,
                                    )
                                }
                            },
                        )
                    },
                    onFailure = { error ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = error.message ?: "Failed to load routine",
                                isEditMode = true,
                            )
                        }
                    },
                )
            }
        }

        private fun saveRoutine() {
            val currentState = _state.value

            if (currentState.title.isBlank()) {
                _state.update { it.copy(error = "Title cannot be empty") }
                return
            }

            if (currentState.steps.isEmpty()) {
                _state.update { it.copy(error = "Add at least one step") }
                return
            }

            if (currentState.steps.any { it.title.isBlank() }) {
                _state.update { it.copy(error = "Step titles cannot be empty") }
                return
            }

            if (
                (
                    currentState.repeatPattern == RepeatPattern.WEEKLY ||
                        currentState.repeatPattern == RepeatPattern.SPECIFIC_DATES
                ) &&
                currentState.scheduledDays.isEmpty()
            ) {
                _state.update { it.copy(error = "Select at least one day") }
                return
            }

            _state.update { it.copy(isSaving = true, error = null) }

            viewModelScope.launch {
                val now = System.currentTimeMillis()
                val editingRoutineId = currentRoutineId
                val routineId = editingRoutineId ?: UUID.randomUUID().toString()
                val today = LocalDate.now()

                val routine =
                    existingRoutine?.copy(
                        title = currentState.title.trim(),
                        description = currentState.description.trim().takeIf { it.isNotBlank() },
                        repeatPattern = currentState.repeatPattern,
                        repeatDays =
                            if (
                                currentState.repeatPattern == RepeatPattern.WEEKLY ||
                                currentState.repeatPattern == RepeatPattern.SPECIFIC_DATES
                            ) {
                                currentState.scheduledDays.toList()
                            } else {
                                null
                            },
                        customInterval =
                            if (currentState.repeatPattern == RepeatPattern.CUSTOM) {
                                currentState.customInterval
                            } else {
                                null
                            },
                        reminderEnabled = currentState.reminderEnabled,
                        reminderTime = currentState.reminderTime,
                        updatedAt = now,
                    ) ?: Routine(
                        id = routineId,
                        title = currentState.title.trim(),
                        description = currentState.description.trim().takeIf { it.isNotBlank() },
                        repeatPattern = currentState.repeatPattern,
                        repeatDays =
                            if (
                                currentState.repeatPattern == RepeatPattern.WEEKLY ||
                                currentState.repeatPattern == RepeatPattern.SPECIFIC_DATES
                            ) {
                                currentState.scheduledDays.toList()
                            } else {
                                null
                            },
                        customInterval =
                            if (currentState.repeatPattern == RepeatPattern.CUSTOM) {
                                currentState.customInterval
                            } else {
                                null
                            },
                        startDate = today,
                        nextScheduledDate = today,
                        completionThreshold = 1.0f,
                        reminderEnabled = currentState.reminderEnabled,
                        reminderTime = currentState.reminderTime,
                    )

                val routineSteps =
                    currentState.steps.mapIndexed { index, item ->
                        existingStepsById[item.id]?.copy(
                            routineId = routine.id,
                            stepOrder = index,
                            title = item.title.trim(),
                            estimatedDurationMinutes = item.estimatedMinutes,
                            updatedAt = now,
                        ) ?: RoutineStep(
                            id = item.id,
                            routineId = routine.id,
                            stepOrder = index,
                            title = item.title.trim(),
                            estimatedDurationMinutes = item.estimatedMinutes,
                        )
                    }

                val result =
                    if (editingRoutineId != null) {
                        routineRepository.updateRoutine(routine, routineSteps)
                    } else {
                        routineRepository.createRoutine(routine, routineSteps)
                    }

                result.fold(
                    onSuccess = {
                        _state.update { it.copy(isSaving = false) }
                        _savedEvent.emit(Unit)
                    },
                    onFailure = { exception ->
                        _state.update {
                            it.copy(
                                isSaving = false,
                                error = exception.message ?: "Failed to save routine",
                            )
                        }
                    },
                )
            }
        }
    }
