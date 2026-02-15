package com.habitao.feature.habits.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habitao.domain.model.Habit
import com.habitao.domain.repository.HabitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * State for Habits Screen (MVI Pattern)
 */
data class HabitsState(
    val habits: List<Habit> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedDate: LocalDate = LocalDate.now()
)

/**
 * Intents (User Actions) for Habits Screen
 */
sealed class HabitsIntent {
    data class LoadHabits(val date: LocalDate) : HabitsIntent()
    data class RefreshHabits : HabitsIntent()
    data class MarkHabitComplete(val habitId: String, val count: Int) : HabitsIntent()
    data class IncrementHabitProgress(val habitId: String) : HabitsIntent()
    data class DeleteHabit(val habitId: String) : HabitsIntent()
    data class ArchiveHabit(val habitId: String) : HabitsIntent()
}

@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val habitRepository: HabitRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HabitsState())
    val state: StateFlow<HabitsState> = _state.asStateFlow()

    init {
        // Load today's habits on init
        processIntent(HabitsIntent.LoadHabits(LocalDate.now()))
    }

    fun processIntent(intent: HabitsIntent) {
        when (intent) {
            is HabitsIntent.LoadHabits -> loadHabits(intent.date)
            is HabitsIntent.RefreshHabits -> refreshHabits()
            is HabitsIntent.MarkHabitComplete -> markHabitComplete(intent.habitId, intent.count)
            is HabitsIntent.IncrementHabitProgress -> incrementHabitProgress(intent.habitId)
            is HabitsIntent.DeleteHabit -> deleteHabit(intent.habitId)
            is HabitsIntent.ArchiveHabit -> archiveHabit(intent.habitId)
        }
    }

    private fun loadHabits(date: LocalDate) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, selectedDate = date) }
            
            habitRepository.getHabitsForDate(date)
                .onSuccess { habits ->
                    _state.update { 
                        it.copy(
                            habits = habits,
                            isLoading = false,
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load habits"
                        )
                    }
                }
        }
    }

    private fun refreshHabits() {
        loadHabits(_state.value.selectedDate)
    }

    private fun markHabitComplete(habitId: String, count: Int) {
        viewModelScope.launch {
            val date = _state.value.selectedDate
            habitRepository.createOrUpdateLog(habitId, date, count)
                .onSuccess {
                    // Refresh the list to show updated progress
                    refreshHabits()
                }
                .onFailure { error ->
                    _state.update { 
                        it.copy(error = error.message ?: "Failed to update habit")
                    }
                }
        }
    }

    private fun incrementHabitProgress(habitId: String) {
        viewModelScope.launch {
            val date = _state.value.selectedDate
            
            // Get current log or create new
            val currentLog = habitRepository.getLogForHabitAndDate(habitId, date).getOrNull()
            val currentCount = currentLog?.currentCount ?: 0
            val newCount = currentCount + 1
            
            markHabitComplete(habitId, newCount)
        }
    }

    private fun deleteHabit(habitId: String) {
        viewModelScope.launch {
            habitRepository.deleteHabit(habitId)
                .onSuccess {
                    refreshHabits()
                }
                .onFailure { error ->
                    _state.update { 
                        it.copy(error = error.message ?: "Failed to delete habit")
                    }
                }
        }
    }

    private fun archiveHabit(habitId: String) {
        viewModelScope.launch {
            habitRepository.archiveHabit(habitId)
                .onSuccess {
                    refreshHabits()
                }
                .onFailure { error ->
                    _state.update { 
                        it.copy(error = error.message ?: "Failed to archive habit")
                    }
                }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
