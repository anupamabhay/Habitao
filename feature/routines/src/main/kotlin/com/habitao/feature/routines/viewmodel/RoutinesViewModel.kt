package com.habitao.feature.routines.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habitao.domain.model.Routine
import com.habitao.domain.model.RoutineLog
import com.habitao.domain.model.RoutineStep
import com.habitao.domain.repository.RoutineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

// State for Routines screen
data class RoutinesState(
    val routines: List<Routine> = emptyList(),
    val steps: Map<String, List<RoutineStep>> = emptyMap(), // routineId -> steps
    val logs: Map<String, RoutineLog> = emptyMap(), // routineId -> log for selected date
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedDate: LocalDate = LocalDate.now(),
)

// Intents for Routines screen
sealed class RoutinesIntent {
    data class SelectDate(val date: LocalDate) : RoutinesIntent()

    data class CreateRoutine(val routine: Routine, val steps: List<RoutineStep>) : RoutinesIntent()

    data class DeleteRoutine(val routineId: String) : RoutinesIntent()

    data class ToggleStep(val routineId: String, val stepId: String, val isCompleted: Boolean) : RoutinesIntent()

    object ClearError : RoutinesIntent()
}

@HiltViewModel
class RoutinesViewModel
    @Inject
    constructor(
        private val routineRepository: RoutineRepository,
    ) : ViewModel() {
        private val selectedDateFlow = MutableStateFlow(LocalDate.now())
        private val errorFlow = MutableStateFlow<String?>(null)

        private val routinesFlow =
            selectedDateFlow.flatMapLatest { date ->
                routineRepository.observeAllRoutines()
                    .map { result ->
                        result.getOrElse { emptyList() }
                            .filter { routine -> routine.isScheduledForDate(date) }
                    }
                    .catch { emit(emptyList()) }
            }

        private val stepsFlow =
            routinesFlow.flatMapLatest { routines ->
                if (routines.isEmpty()) {
                    flowOf(emptyMap())
                } else {
                    val stepFlows =
                        routines.map { routine ->
                            routineRepository.observeRoutineSteps(routine.id)
                                .map { result -> routine.id to result.getOrElse { emptyList() } }
                        }
                    combine(stepFlows) { pairs -> pairs.toMap() }
                }
            }

        private val logsFlow =
            combine(routinesFlow, selectedDateFlow) { routines, date -> routines to date }
                .flatMapLatest { (routines, date) ->
                    if (routines.isEmpty()) {
                        flowOf(emptyMap())
                    } else {
                        val logFlows =
                            routines.map { routine ->
                                routineRepository.observeRoutineLog(routine.id, date)
                                    .map { result -> routine.id to result.getOrNull() }
                            }
                        combine(logFlows) { pairs ->
                            pairs.mapNotNull { (id, log) -> log?.let { id to it } }.toMap()
                        }
                    }
                }

        val state: StateFlow<RoutinesState> =
            combine(
                selectedDateFlow,
                routinesFlow,
                stepsFlow,
                logsFlow,
                errorFlow,
            ) { date, routines, steps, logs, error ->
                RoutinesState(
                    routines = routines,
                    steps = steps,
                    logs = logs,
                    isLoading = false,
                    error = error,
                    selectedDate = date,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = RoutinesState(),
            )

        fun processIntent(intent: RoutinesIntent) {
            when (intent) {
                is RoutinesIntent.SelectDate -> selectDate(intent.date)
                is RoutinesIntent.CreateRoutine -> createRoutine(intent.routine, intent.steps)
                is RoutinesIntent.DeleteRoutine -> deleteRoutine(intent.routineId)
                is RoutinesIntent.ToggleStep -> toggleStep(intent.routineId, intent.stepId, intent.isCompleted)
                is RoutinesIntent.ClearError -> clearError()
            }
        }

        fun refreshDate() {
            val today = LocalDate.now()
            if (selectedDateFlow.value != today) {
                selectedDateFlow.value = today
            }
        }

        private fun selectDate(date: LocalDate) {
            selectedDateFlow.value = date
        }

        private fun createRoutine(
            routine: Routine,
            steps: List<RoutineStep>,
        ) {
            viewModelScope.launch {
                val saveResult =
                    routineRepository.getRoutineById(routine.id).fold(
                        onSuccess = { existingRoutine ->
                            routineRepository.upsertRoutine(
                                routine.copy(
                                    createdAt = existingRoutine.createdAt,
                                    startDate = existingRoutine.startDate,
                                    nextScheduledDate = existingRoutine.nextScheduledDate,
                                    updatedAt = System.currentTimeMillis(),
                                ),
                                steps,
                            )
                        },
                        onFailure = {
                            routineRepository.upsertRoutine(routine, steps)
                        },
                    )

                saveResult.onFailure { error ->
                    errorFlow.value = error.message ?: "Failed to save routine"
                }
            }
        }

        private fun deleteRoutine(routineId: String) {
            viewModelScope.launch {
                routineRepository.deleteRoutine(routineId)
                    .onFailure { error ->
                        errorFlow.value = error.message ?: "Failed to delete routine"
                    }
            }
        }

        private fun toggleStep(
            routineId: String,
            stepId: String,
            isCompleted: Boolean,
        ) {
            viewModelScope.launch {
                val date = selectedDateFlow.value
                routineRepository.logRoutineStep(routineId, stepId, date, isCompleted)
                    .onFailure { error ->
                        errorFlow.value = error.message ?: "Failed to update step"
                    }
            }
        }

        private fun clearError() {
            errorFlow.value = null
        }
    }
