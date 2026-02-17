package com.habitao.feature.habits.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habitao.domain.model.Habit
import com.habitao.domain.model.HabitLog
import com.habitao.domain.repository.HabitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * State for Habits Screen (MVI Pattern)
 */
data class HabitsState(
    val habits: List<Habit> = emptyList(),
    val logs: Map<String, HabitLog> = emptyMap(), // habitId -> log for selected date
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedDate: LocalDate = LocalDate.now()
)

/**
 * Intents (User Actions) for Habits Screen
 */
sealed class HabitsIntent {
    data class SelectDate(val date: LocalDate) : HabitsIntent()
    data class MarkHabitComplete(val habitId: String, val count: Int) : HabitsIntent()
    data class IncrementHabitProgress(val habitId: String) : HabitsIntent()
    data class DeleteHabit(val habitId: String) : HabitsIntent()
    data class ArchiveHabit(val habitId: String) : HabitsIntent()
}

@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val habitRepository: HabitRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _error = MutableStateFlow<String?>(null)

    /**
     * Observe habits reactively - auto-updates when database changes.
     * This fixes the refresh bug where habits wouldn't appear after creation.
     */
    val state: StateFlow<HabitsState> = combine(
        _selectedDate,
        _selectedDate.flatMapLatest { date ->
            habitRepository.observeHabitsForDate(date)
                .map { result ->
                    result.getOrElse { emptyList() }
                }
                .catch { emit(emptyList()) }
        },
        _selectedDate.flatMapLatest { date ->
            habitRepository.observeLogsForDate(date)
                .map { result ->
                    result.getOrElse { emptyMap() }
                }
                .catch { emit(emptyMap()) }
        },
        _error
    ) { date, habits, logs, error ->
        HabitsState(
            habits = habits,
            logs = logs,
            isLoading = false,
            error = error,
            selectedDate = date
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HabitsState()
    )

    fun processIntent(intent: HabitsIntent) {
        when (intent) {
            is HabitsIntent.SelectDate -> selectDate(intent.date)
            is HabitsIntent.MarkHabitComplete -> markHabitComplete(intent.habitId, intent.count)
            is HabitsIntent.IncrementHabitProgress -> incrementHabitProgress(intent.habitId)
            is HabitsIntent.DeleteHabit -> deleteHabit(intent.habitId)
            is HabitsIntent.ArchiveHabit -> archiveHabit(intent.habitId)
        }
    }

    private fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    private fun markHabitComplete(habitId: String, count: Int) {
        viewModelScope.launch {
            val date = _selectedDate.value
            habitRepository.createOrUpdateLog(habitId, date, count)
                .onFailure { error ->
                    _error.value = error.message ?: "Failed to update habit"
                }
            // No need to manually refresh - Flow observation handles it
        }
    }

    private fun incrementHabitProgress(habitId: String) {
        viewModelScope.launch {
            val date = _selectedDate.value

            // Get current log or create new
            val currentLog = habitRepository.getLogForHabitAndDate(habitId, date).getOrNull()
            val currentValue = currentLog?.currentValue ?: 0
            val newValue = currentValue + 1

            markHabitComplete(habitId, newValue)
        }
    }

    private fun deleteHabit(habitId: String) {
        viewModelScope.launch {
            habitRepository.deleteHabit(habitId)
                .onFailure { error ->
                    _error.value = error.message ?: "Failed to delete habit"
                }
            // No need to manually refresh - Flow observation handles it
        }
    }

    private fun archiveHabit(habitId: String) {
        viewModelScope.launch {
            habitRepository.archiveHabit(habitId)
                .onFailure { error ->
                    _error.value = error.message ?: "Failed to archive habit"
                }
            // No need to manually refresh - Flow observation handles it
        }
    }

    fun clearError() {
        _error.value = null
    }
}
