package com.habitao.feature.habits.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habitao.domain.model.StreakInfo
import com.habitao.domain.repository.HabitRepository
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

/**
 * Stats for a single habit, combining identity with streak data.
 */
data class HabitStatItem(
    val habitId: String,
    val title: String,
    val currentStreak: Int,
    val longestStreak: Int,
    val totalCompletions: Int,
    val isCompletedToday: Boolean,
)

/**
 * State for the Stats screen.
 */
data class StatsState(
    val totalHabits: Int = 0,
    val completedToday: Int = 0,
    val overallCompletionRate: Float = 0f,
    val currentBestStreak: Int = 0,
    val habitStats: List<HabitStatItem> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class StatsViewModel
    @Inject
    constructor(
        private val habitRepository: HabitRepository,
    ) : ViewModel() {
        private val today = LocalDate.now()
        private val statsDataFlow = MutableStateFlow<List<HabitStatItem>>(emptyList())
        private val isLoadingFlow = MutableStateFlow(true)

        val state: StateFlow<StatsState> =
            combine(
                habitRepository.observeHabitsForDate(today)
                    .map { result -> result.getOrElse { emptyList() } }
                    .catch { emit(emptyList()) },
                habitRepository.observeLogsForDate(today)
                    .map { result -> result.getOrElse { emptyMap() } }
                    .catch { emit(emptyMap()) },
                statsDataFlow,
                isLoadingFlow,
            ) { habits, logs, statItems, isLoading ->
                val completedToday = logs.count { it.value.isCompleted }
                val totalHabits = habits.size
                val rate =
                    if (totalHabits > 0) {
                        completedToday.toFloat() / totalHabits
                    } else {
                        0f
                    }

                StatsState(
                    totalHabits = totalHabits,
                    completedToday = completedToday,
                    overallCompletionRate = rate,
                    currentBestStreak = statItems.maxOfOrNull { it.currentStreak } ?: 0,
                    habitStats = statItems,
                    isLoading = isLoading,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = StatsState(),
            )

        init {
            loadHabitStats()
        }

        private fun loadHabitStats() {
            viewModelScope.launch {
                isLoadingFlow.value = true
                combine(
                    habitRepository.observeAllHabits()
                        .map { result -> result.getOrElse { emptyList() } }
                        .catch { emit(emptyList()) },
                    habitRepository.observeLogsForDate(today)
                        .map { result -> result.getOrElse { emptyMap() } }
                        .catch { emit(emptyMap()) },
                ) { habits, logs ->
                    habits to logs
                }.collect { (habits, logs) ->
                    val items =
                        habits.map { habit ->
                            val streak = loadStreakSafe(habit.id)
                            HabitStatItem(
                                habitId = habit.id,
                                title = habit.title,
                                currentStreak = streak.currentStreak,
                                longestStreak = streak.longestStreak,
                                totalCompletions = streak.totalCompletions,
                                isCompletedToday = logs[habit.id]?.isCompleted == true,
                            )
                        }
                    statsDataFlow.value = items
                    isLoadingFlow.value = false
                }
            }
        }

        private suspend fun loadStreakSafe(habitId: String): StreakInfo {
            return habitRepository.calculateStreak(habitId)
                .getOrElse { StreakInfo(0, 0, 0) }
        }
    }
