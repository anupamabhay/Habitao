package com.habitao.feature.routines.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habitao.domain.repository.RoutineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject

data class RoutineStatItem(
    val routineId: String,
    val title: String,
    val completionCount: Int,
    val totalScheduledDays: Int,
    val completionRate: Float,
    val currentStreak: Int,
    val longestStreak: Int,
)

data class RoutineActivityPoint(
    val label: String,
    val completedCount: Int,
    val totalCount: Int,
)

data class RoutineStatsState(
    val totalRoutines: Int = 0,
    val completedToday: Int = 0,
    val overallCompletionRate: Float = 0f,
    val routineStats: List<RoutineStatItem> = emptyList(),
    val activityData: List<RoutineActivityPoint> = emptyList(),
    val timeFilter: Int = 0,
    val isLoading: Boolean = true,
)

private data class RoutineStatsDateRange(
    val startDate: LocalDate,
    val endDate: LocalDate,
) {
    val totalDays: Int = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1

    fun allDates(): List<LocalDate> =
        (0 until totalDays).map { offset ->
            startDate.plusDays(offset.toLong())
        }
}

@HiltViewModel
class RoutineStatsViewModel
    @Inject
    constructor(
        private val routineRepository: RoutineRepository,
    ) : ViewModel() {
        private val dateLabelFormatter = DateTimeFormatter.ofPattern("M/d", Locale.getDefault())
        private val timeFilterFlow = MutableStateFlow(0)

        private val dateRangeFlow =
            timeFilterFlow
                .map { filter -> getDateRangeForFilter(filter) }
                .distinctUntilChanged()

        private val routinesFlow =
            routineRepository.observeAllRoutines()
                .map { result -> result.getOrElse { emptyList() } }
                .catch { emit(emptyList()) }

        private val routineLogsFlow =
            dateRangeFlow.flatMapLatest { range ->
                routineRepository.observeRoutineLogsForDateRange(range.startDate, range.endDate)
            }
                .map { result -> result.getOrElse { emptyList() } }
                .catch { emit(emptyList()) }

        val state: StateFlow<RoutineStatsState> =
            combine(timeFilterFlow, dateRangeFlow, routinesFlow, routineLogsFlow) { timeFilter, range, routines, logs ->
                val today = LocalDate.now()
                val allDates = range.allDates()
                val completedPairs =
                    logs
                        .asSequence()
                        .filter { it.isCompleted }
                        .map { it.routineId to it.date }
                        .toSet()

                val completedByDate = completedPairs.groupingBy { it.second }.eachCount()

                val routineStats =
                    routines
                        .map { routine ->
                            val completionDates =
                                completedPairs
                                    .asSequence()
                                    .filter { (routineId, _) -> routineId == routine.id }
                                    .map { (_, date) -> date }
                                    .toSet()

                            val completionCount = completionDates.size
                            val totalScheduledDays = allDates.count { date -> routine.isScheduledForDate(date) }
                            val completionRate =
                                if (totalScheduledDays > 0) {
                                    completionCount.toFloat() / totalScheduledDays
                                } else {
                                    0f
                                }

                            val (currentStreak, longestStreak) =
                                calculateStreaks(
                                    completedDates = completionDates,
                                    datesInRange = allDates,
                                )

                            RoutineStatItem(
                                routineId = routine.id,
                                title = routine.title,
                                completionCount = completionCount,
                                totalScheduledDays = totalScheduledDays,
                                completionRate = completionRate,
                                currentStreak = currentStreak,
                                longestStreak = longestStreak,
                            )
                        }
                        .sortedByDescending { item -> item.completionRate }

                val totalScheduledDays = routineStats.sumOf { item -> item.totalScheduledDays }
                val totalCompletions = routineStats.sumOf { item -> item.completionCount }

                val activityData =
                    allDates.map { date ->
                        RoutineActivityPoint(
                            label = formatLabelForDate(date = date, timeFilter = timeFilter),
                            completedCount = completedByDate[date] ?: 0,
                            totalCount = routines.count { routine -> routine.isScheduledForDate(date) },
                        )
                    }

                RoutineStatsState(
                    totalRoutines = routines.size,
                    completedToday = completedPairs.count { (_, date) -> date == today },
                    overallCompletionRate =
                        if (totalScheduledDays > 0) {
                            totalCompletions.toFloat() / totalScheduledDays
                        } else {
                            0f
                        },
                    routineStats = routineStats,
                    activityData = activityData,
                    timeFilter = timeFilter,
                    isLoading = false,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = RoutineStatsState(),
            )

        fun onTimeFilterChanged(filter: Int) {
            if (filter in 0..2) {
                timeFilterFlow.value = filter
            }
        }

        private fun getDateRangeForFilter(filter: Int): RoutineStatsDateRange {
            val today = LocalDate.now()
            return when (filter) {
                1 -> RoutineStatsDateRange(startDate = today.minusDays(29), endDate = today)
                2 -> RoutineStatsDateRange(startDate = today.minusDays(364), endDate = today)
                else -> RoutineStatsDateRange(startDate = today.minusDays(6), endDate = today)
            }
        }

        private fun formatLabelForDate(
            date: LocalDate,
            timeFilter: Int,
        ): String =
            when (timeFilter) {
                0 -> date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                else -> date.format(dateLabelFormatter)
            }

        private fun calculateStreaks(
            completedDates: Set<LocalDate>,
            datesInRange: List<LocalDate>,
        ): Pair<Int, Int> {
            var longestStreak = 0
            var runningStreak = 0

            datesInRange.forEach { date ->
                if (completedDates.contains(date)) {
                    runningStreak += 1
                    longestStreak = maxOf(longestStreak, runningStreak)
                } else {
                    runningStreak = 0
                }
            }

            var currentStreak = 0
            for (index in datesInRange.lastIndex downTo 0) {
                if (completedDates.contains(datesInRange[index])) {
                    currentStreak += 1
                } else {
                    break
                }
            }

            return currentStreak to longestStreak
        }
    }
