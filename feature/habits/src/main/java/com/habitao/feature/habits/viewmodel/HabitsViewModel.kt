package com.habitao.feature.habits.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habitao.domain.model.FrequencyType
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
 * Available sorting options for the habits list.
 */
enum class SortOption {
    MANUAL,
    ALPHABETICAL,
    NEWEST_FIRST,
    OLDEST_FIRST,
    BY_COMPLETION,
}

/**
 * State for Habits Screen (MVI Pattern)
 */
data class HabitsState(
    val habits: List<Habit> = emptyList(),
    val logs: Map<String, HabitLog> = emptyMap(), // habitId -> log for selected date
    val streaks: Map<String, Int> = emptyMap(), // habitId -> current streak
    val weeklyProgress: Map<String, Int> = emptyMap(), // habitId -> weekly progress for period-based habits
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedDate: LocalDate = LocalDate.now(),
    val sortOption: SortOption = SortOption.MANUAL,
)

/**
 * Intents (User Actions) for Habits Screen
 */
sealed class HabitsIntent {
    data class SelectDate(val date: LocalDate) : HabitsIntent()

    data class MarkHabitComplete(val habitId: String, val count: Int) : HabitsIntent()

    data class IncrementHabitProgress(val habitId: String) : HabitsIntent()

    data class DecrementHabitProgress(val habitId: String) : HabitsIntent()

    data class DeleteHabit(val habitId: String) : HabitsIntent()

    data class ArchiveHabit(val habitId: String) : HabitsIntent()

    data class ToggleChecklistItem(val habitId: String, val itemId: String) : HabitsIntent()

    data class SetSortOption(val sortOption: SortOption) : HabitsIntent()
}

@HiltViewModel
class HabitsViewModel
    @Inject
    constructor(
        private val habitRepository: HabitRepository,
    ) : ViewModel() {
    private val selectedDateFlow = MutableStateFlow(LocalDate.now())
    private val errorFlow = MutableStateFlow<String?>(null)
    private val sortOptionFlow = MutableStateFlow(SortOption.MANUAL)
    private val streaksFlow = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val weeklyProgressFlow = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val streakRefreshTrigger = MutableStateFlow(0L)

        /**
         * Observe habits reactively - auto-updates when database changes.
         * This fixes the refresh bug where habits wouldn't appear after creation.
         */
        val state: StateFlow<HabitsState> =
            combine(
                selectedDateFlow,
                selectedDateFlow.flatMapLatest { date ->
                    habitRepository.observeHabitsForDate(date)
                        .map { result ->
                            result.getOrElse { emptyList() }
                        }
                        .catch { emit(emptyList()) }
                },
                selectedDateFlow.flatMapLatest { date ->
                    habitRepository.observeLogsForDate(date)
                        .map { result ->
                            result.getOrElse { emptyMap() }
                        }
                        .catch { emit(emptyMap()) }
                },
                errorFlow,
                sortOptionFlow,
                streaksFlow,
                weeklyProgressFlow,
            ) { values ->
                val date = values[0] as LocalDate

                @Suppress("UNCHECKED_CAST")
                val habits = values[1] as List<Habit>

                @Suppress("UNCHECKED_CAST")
                val logs = values[2] as Map<String, HabitLog>

                val error = values[3] as String?
                val sortOption = values[4] as SortOption

                @Suppress("UNCHECKED_CAST")
                val streaks = values[5] as Map<String, Int>

                @Suppress("UNCHECKED_CAST")
                val weeklyProgress = values[6] as Map<String, Int>

                val sortedHabits = applySorting(habits, logs, sortOption)

                HabitsState(
                    habits = sortedHabits,
                    logs = logs,
                    streaks = streaks,
                    weeklyProgress = weeklyProgress,
                    isLoading = false,
                    error = error,
                    selectedDate = date,
                    sortOption = sortOption,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = HabitsState(),
            )

        init {
            loadStreaks()
            loadWeeklyProgress()
        }

        fun processIntent(intent: HabitsIntent) {
            when (intent) {
                is HabitsIntent.SelectDate -> selectDate(intent.date)
                is HabitsIntent.MarkHabitComplete -> markHabitComplete(intent.habitId, intent.count)
                is HabitsIntent.IncrementHabitProgress -> incrementHabitProgress(intent.habitId)
                is HabitsIntent.DecrementHabitProgress -> decrementHabitProgress(intent.habitId)
                is HabitsIntent.DeleteHabit -> deleteHabit(intent.habitId)
                is HabitsIntent.ArchiveHabit -> archiveHabit(intent.habitId)
                is HabitsIntent.ToggleChecklistItem -> toggleChecklistItem(intent.habitId, intent.itemId)
                is HabitsIntent.SetSortOption -> setSortOption(intent.sortOption)
            }
        }

        private fun setSortOption(option: SortOption) {
            sortOptionFlow.value = option
        }

        private fun applySorting(
            habits: List<Habit>,
            logs: Map<String, HabitLog>,
            sortOption: SortOption,
        ): List<Habit> {
            return when (sortOption) {
                SortOption.MANUAL -> habits // Already sorted by sortOrder ASC, createdAt DESC from DAO
                SortOption.ALPHABETICAL -> habits.sortedBy { it.title.lowercase() }
                SortOption.NEWEST_FIRST -> habits.sortedByDescending { it.createdAt }
                SortOption.OLDEST_FIRST -> habits.sortedBy { it.createdAt }
                SortOption.BY_COMPLETION -> habits.sortedBy { logs[it.id]?.isCompleted == true }
            }
        }

        private fun loadStreaks() {
            viewModelScope.launch {
                // Observe habits from DB directly to avoid circular dependency with state
            combine(selectedDateFlow, streakRefreshTrigger) { date, _ -> date }
                .flatMapLatest { date ->
                    habitRepository.observeHabitsForDate(date)
                        .map { result -> result.getOrElse { emptyList() } }
                        .catch { emit(emptyList()) }
                }.collect { habits ->
                    if (habits.isNotEmpty()) {
                        val newStreaks = mutableMapOf<String, Int>()
                        for (habit in habits) {
                            habitRepository.calculateStreak(habit.id).onSuccess { info ->
                                if (info.currentStreak > 0) {
                                    newStreaks[habit.id] = info.currentStreak
                                }
                            }
                        }
                        streaksFlow.value = newStreaks
                    } else {
                        streaksFlow.value = emptyMap()
                    }
                }
            }
        }

        private fun loadWeeklyProgress() {
            viewModelScope.launch {
                combine(selectedDateFlow, streakRefreshTrigger) { date, _ -> date }
                    .flatMapLatest { date ->
                        habitRepository.observeHabitsForDate(date)
                            .map { result -> result.getOrElse { emptyList() } to date }
                            .catch { emit(emptyList<Habit>() to date) }
                    }.collect { (habits, date) ->
                        if (habits.isNotEmpty()) {
                            val newWeeklyProgress = mutableMapOf<String, Int>()
                            for (habit in habits) {
                                // Fetch progress for period-based habits (weekly or cycle-based)
                                if (habit.frequencyType == FrequencyType.TIMES_PER_WEEK ||
                                    habit.frequencyType == FrequencyType.EVERY_X_DAYS) {
                                    habitRepository.getWeeklyProgressForHabit(habit.id, date)
                                        .onSuccess { progress ->
                                            newWeeklyProgress[habit.id] = progress
                                        }
                                }
                            }
                            weeklyProgressFlow.value = newWeeklyProgress
                        } else {
                            weeklyProgressFlow.value = emptyMap()
                        }
                    }
            }
        }

        private fun selectDate(date: LocalDate) {
            selectedDateFlow.value = date
        }

        private fun markHabitComplete(
            habitId: String,
            count: Int,
        ) {
            viewModelScope.launch {
                val date = selectedDateFlow.value
                habitRepository.createOrUpdateLog(habitId, date, count)
                    .onFailure { error ->
                        errorFlow.value = error.message ?: "Failed to update habit"
                    }
                streakRefreshTrigger.value = System.currentTimeMillis()
                // No need to manually refresh - Flow observation handles it
            }
        }

        private fun incrementHabitProgress(habitId: String) {
            viewModelScope.launch {
                val date = selectedDateFlow.value

                // Get current log or create new
                val currentLog = habitRepository.getLogForHabitAndDate(habitId, date).getOrNull()
                val currentValue = currentLog?.currentValue ?: 0
                val newValue = currentValue + 1

                habitRepository.createOrUpdateLog(habitId, date, newValue)
                    .onFailure { error ->
                        errorFlow.value = error.message ?: "Failed to update habit"
                    }
                streakRefreshTrigger.value = System.currentTimeMillis()
            }
        }

        private fun decrementHabitProgress(habitId: String) {
            viewModelScope.launch {
                val date = selectedDateFlow.value

                // Get current log or create new
                val currentLog = habitRepository.getLogForHabitAndDate(habitId, date).getOrNull()
                val currentValue = currentLog?.currentValue ?: 0
                val newValue = maxOf(0, currentValue - 1)

                habitRepository.createOrUpdateLog(habitId, date, newValue)
                    .onFailure { error ->
                        errorFlow.value = error.message ?: "Failed to update habit"
                    }
                streakRefreshTrigger.value = System.currentTimeMillis()
            }
        }

        private fun deleteHabit(habitId: String) {
            viewModelScope.launch {
                habitRepository.deleteHabit(habitId)
                    .onFailure { error ->
                        errorFlow.value = error.message ?: "Failed to delete habit"
                    }
                // No need to manually refresh - Flow observation handles it
            }
        }

        private fun archiveHabit(habitId: String) {
            viewModelScope.launch {
                habitRepository.archiveHabit(habitId)
                    .onFailure { error ->
                        errorFlow.value = error.message ?: "Failed to archive habit"
                    }
                // No need to manually refresh - Flow observation handles it
            }
        }

        private fun toggleChecklistItem(
            habitId: String,
            itemId: String,
        ) {
            viewModelScope.launch {
                val date = selectedDateFlow.value
                habitRepository.toggleChecklistItem(habitId, date, itemId)
                    .onFailure { error ->
                        errorFlow.value = error.message ?: "Failed to update checklist item"
                    }
                streakRefreshTrigger.value = System.currentTimeMillis()
            }
        }

        fun clearError() {
            errorFlow.value = null
        }
    }
