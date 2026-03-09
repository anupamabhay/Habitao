package com.habitao.feature.habits.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habitao.core.datastore.AppSettingsManager
import com.habitao.domain.model.FrequencyType
import com.habitao.domain.model.PomodoroSession
import com.habitao.domain.model.PomodoroType
import com.habitao.domain.model.RoutineLog
import com.habitao.domain.model.StreakInfo
import com.habitao.domain.model.Task
import com.habitao.domain.repository.HabitRepository
import com.habitao.domain.repository.PomodoroRepository
import com.habitao.domain.repository.RoutineRepository
import com.habitao.domain.repository.TaskRepository
import com.habitao.feature.pomodoro.service.PomodoroPreferences
import com.habitao.feature.pomodoro.service.TimerState
import com.habitao.feature.pomodoro.service.TimerStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject

@Immutable
data class HabitStatItem(
    val habitId: String,
    val title: String,
    val currentStreak: Int,
    val longestStreak: Int,
    val totalCompletions: Int,
    val isCompletedToday: Boolean,
    val frequency: String = "Daily",
)

@Immutable
data class ActivityDataPoint(
    val label: String,
    val habitsCompleted: Int,
    val routinesCompleted: Int,
    val tasksCompleted: Int,
)

@Immutable
data class StatsState(
    val totalHabits: Int = 0,
    val completedToday: Int = 0,
    val overallCompletionRate: Float = 0f,
    val currentBestStreak: Int = 0,
    val habitStats: List<HabitStatItem> = emptyList(),
    val todaysFocusSeconds: Int = 0,
    val todaysPomodoroSessions: Int = 0,
    val todaysCompletedRounds: Int = 0,
    val completedTasksToday: Int = 0,
    val totalTasks: Int = 0,
    val taskCompletionRate: Float = 0f,
    val completedRoutinesToday: Int = 0,
    val totalRoutines: Int = 0,
    val routineCompletionRate: Float = 0f,
    val timeFilter: Int = 0,
    val activityData: List<ActivityDataPoint> = emptyList(),
    val graphType: String = "BAR",
    val isLoading: Boolean = true,
)

private data class StatsDateRange(
    val startDate: LocalDate,
    val endDate: LocalDate,
) {
    val totalDays: Int = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1

    fun allDates(): List<LocalDate> = (0 until totalDays).map { startDate.plusDays(it.toLong()) }
}

private data class TaskStatsData(
    val completed: Int,
    val total: Int,
    val completionRate: Float,
    val completedByDate: Map<LocalDate, Int>,
    val completedByHour: Map<Int, Int>,
)

private data class RoutineStatsData(
    val completed: Int,
    val total: Int,
    val completionRate: Float,
)

private data class PomodoroStatsData(
    val sessions: List<PomodoroSession>,
    val activeWorkSeconds: Int,
)

private data class CoreStatsInput(
    val range: StatsDateRange,
    val fetchRange: StatsDateRange,
    val habits: List<com.habitao.domain.model.Habit>,
    val habitLogs: List<com.habitao.domain.model.HabitLog>,
    val routineLogs: List<RoutineLog>,
    val taskStats: TaskStatsData,
)

@HiltViewModel
class StatsViewModel
    @Inject
    constructor(
        private val habitRepository: HabitRepository,
        private val pomodoroRepository: PomodoroRepository,
        private val taskRepository: TaskRepository,
        private val routineRepository: RoutineRepository,
        private val timerStateHolder: TimerStateHolder,
        private val pomodoroPreferences: PomodoroPreferences,
        private val appSettingsManager: AppSettingsManager,
    ) : ViewModel() {
        private val today = LocalDate.now()
        private val timeFilterFlow = MutableStateFlow(0)

        private val displayDateRangeFlow =
            timeFilterFlow
                .map { filter -> getDateRangeForFilter(filter) }
                .distinctUntilChanged()

        private val fetchDateRangeFlow =
            timeFilterFlow
                .map { filter ->
                    when (filter) {
                        0 -> StatsDateRange(startDate = today, endDate = today)
                        2 -> StatsDateRange(startDate = today.minusDays(29), endDate = today)
                        else -> StatsDateRange(startDate = today.minusDays(6), endDate = today)
                    }
                }
                .distinctUntilChanged()

        private val habitsFlow =
            habitRepository.observeAllHabits()
                .map { result -> result.getOrElse { emptyList() } }
                .catch { emit(emptyList()) }
                .distinctUntilChanged()

        private val habitLogsFlow =
            fetchDateRangeFlow.flatMapLatest { range ->
                habitRepository.observeLogsForDateRange(range.startDate, range.endDate)
            }
                .map { result -> result.getOrElse { emptyList() } }
                .catch { emit(emptyList()) }
                .distinctUntilChanged()

        private val tasksFlow =
            fetchDateRangeFlow.flatMapLatest { range ->
                taskRepository.observeTasksForDateRange(range.startDate, range.endDate)
            }
                .map { result -> result.getOrElse { emptyList() } }
                .catch { emit(emptyList()) }
                .distinctUntilChanged()

        private val routinesFlow =
            routineRepository.observeAllRoutines()
                .map { result -> result.getOrElse { emptyList() } }
                .catch { emit(emptyList()) }
                .distinctUntilChanged()

        private val routineLogsFlow =
            fetchDateRangeFlow.flatMapLatest { range ->
                routineRepository.observeRoutineLogsForDateRange(range.startDate, range.endDate)
            }
                .map { result -> result.getOrElse { emptyList() } }
                .catch { emit(emptyList()) }
                .distinctUntilChanged()

        private val pomodoroSessionsFlow =
            fetchDateRangeFlow.flatMapLatest { range ->
                pomodoroRepository.observeSessionsForDateRange(range.startDate, range.endDate)
            }
                .map { result -> result.getOrElse { emptyList() } }
                .catch { emit(emptyList()) }
                .distinctUntilChanged()

        private val activeWorkSecondsFlow =
            combine(
                timerStateHolder.totalSeconds,
                timerStateHolder.remainingSeconds,
                timerStateHolder.currentSessionType,
                timerStateHolder.timerState,
            ) { totalSeconds, remainingSeconds, sessionType, timerState ->
                if (
                    sessionType == PomodoroType.WORK &&
                    totalSeconds > 0L &&
                    (timerState == TimerState.RUNNING || timerState == TimerState.PAUSED)
                ) {
                    (totalSeconds - remainingSeconds).coerceAtLeast(0L).toInt()
                } else {
                    0
                }
            }

        private val habitStatsFlow =
            combine(habitsFlow, habitLogsFlow) { habits, logs ->
                habits to logs
            }.mapLatest { (habits, logs) ->
                withContext(Dispatchers.Default) {
                    val completedTodayIds =
                        logs
                            .filter { it.date == today && it.isCompleted }
                            .map { it.habitId }
                            .toSet()

                    // Load streaks concurrently — DB calls dispatch to IO internally.
                    coroutineScope {
                        habits
                            .map { habit ->
                                async {
                                    val streak = loadStreakSafe(habit.id)
                                    val frequency =
                                        when (habit.frequencyType) {
                                            FrequencyType.DAILY -> "Daily"
                                            FrequencyType.SPECIFIC_DAYS -> {
                                                if (habit.scheduledDays.size == 7) {
                                                    "Daily"
                                                } else {
                                                    habit.scheduledDays
                                                        .sorted()
                                                        .joinToString(", ") { it.shortName }
                                                        .ifBlank { "Weekly" }
                                                }
                                            }
                                            FrequencyType.TIMES_PER_WEEK ->
                                                "${habit.frequencyValue}x/week"
                                            FrequencyType.EVERY_X_DAYS ->
                                                "Every ${habit.frequencyValue} days"
                                        }
                                    HabitStatItem(
                                        habitId = habit.id,
                                        title = habit.title,
                                        currentStreak = streak.currentStreak,
                                        longestStreak = streak.longestStreak,
                                        totalCompletions = streak.totalCompletions,
                                        isCompletedToday =
                                            completedTodayIds.contains(habit.id),
                                        frequency = frequency,
                                    )
                                }
                            }
                            .awaitAll()
                            .filter { it.currentStreak >= 2 }
                            .sortedByDescending { it.currentStreak }
                    }
                }
            }

        private val taskStatsFlow =
            combine(tasksFlow, displayDateRangeFlow) { tasks, range ->
                buildTaskStats(tasks, range)
            }

        private val routineStatsFlow =
            combine(routineLogsFlow, routinesFlow, displayDateRangeFlow) { logs, routines, range ->
                val completed =
                    logs.count {
                        it.isCompleted &&
                            !it.date.isBefore(
                                range.startDate,
                            ) && !it.date.isAfter(range.endDate)
                    }
                val total = routines.size * range.totalDays
                val rate = if (total > 0) completed.toFloat() / total else 0f
                RoutineStatsData(completed = completed, total = total, completionRate = rate)
            }

        private val pomodoroStatsFlow =
            combine(
                pomodoroSessionsFlow,
                activeWorkSecondsFlow,
                displayDateRangeFlow,
            ) { sessions, activeWorkSeconds, range ->
                val filteredSessions =
                    sessions.filter {
                        val date = it.startedAt.toLocalDate(ZoneId.systemDefault())
                        !date.isBefore(range.startDate) && !date.isAfter(range.endDate)
                    }
                PomodoroStatsData(
                    sessions = filteredSessions,
                    activeWorkSeconds = activeWorkSeconds,
                )
            }

        private val coreStatsFlow =
            combine(
                combine(displayDateRangeFlow, fetchDateRangeFlow, habitsFlow) { r, fr, h -> Triple(r, fr, h) },
                combine(habitLogsFlow, routineLogsFlow, taskStatsFlow) { hl, rl, ts -> Triple(hl, rl, ts) },
            ) { (range, fetchRange, habits), (habitLogs, routineLogs, taskStats) ->
                CoreStatsInput(
                    range = range,
                    fetchRange = fetchRange,
                    habits = habits,
                    habitLogs = habitLogs,
                    routineLogs = routineLogs,
                    taskStats = taskStats,
                )
            }

        private val graphTypeFlow = appSettingsManager.settings.map { it.statsGraphType }.distinctUntilChanged()

        val state: StateFlow<StatsState> =
            combine(
                combine(timeFilterFlow, coreStatsFlow, habitStatsFlow) { tf, cs, hs -> Triple(tf, cs, hs) },
                combine(pomodoroStatsFlow, routineStatsFlow, graphTypeFlow) { ps, rs, gt -> Triple(ps, rs, gt) },
            ) { (timeFilter, coreStats, habitStats), (pomodoroStats, routineStats, graphType) ->
                val range = coreStats.range
                val fetchRange = coreStats.fetchRange
                val habits = coreStats.habits
                val habitLogs = coreStats.habitLogs
                val routineLogs = coreStats.routineLogs
                val taskStats = coreStats.taskStats
                val pomodoroSessions = pomodoroStats.sessions

                val habitById = habits.associateBy { it.id }
                val dates = range.allDates()
                val fetchDates = fetchRange.allDates()

                val completedHabits =
                    habitLogs.count { log ->
                        log.isCompleted &&
                            !log.date.isBefore(range.startDate) &&
                            !log.date.isAfter(range.endDate) &&
                            habitById[log.habitId]?.isScheduledFor(log.date) == true
                    }

                val totalHabitTargets =
                    dates.sumOf { date ->
                        habits.count { it.isScheduledFor(date) }
                    }

                val habitRate = if (totalHabitTargets > 0) completedHabits.toFloat() / totalHabitTargets else 0f

                val workSessions = pomodoroSessions.filter { it.sessionType == PomodoroType.WORK }
                val focusSeconds =
                    workSessions.sumOf { it.actualDurationSeconds ?: 0 } + pomodoroStats.activeWorkSeconds
                val completedWorkSessions =
                    workSessions.count { (it.actualDurationSeconds ?: 0) > 0 }
                val completedRounds =
                    if (timeFilter == 0) {
                        pomodoroPreferences.getTodaysRounds()
                    } else {
                        completedWorkSessions
                    }

                val activityData =
                    if (timeFilter == 0) {
                        buildHourlyActivityData(
                            habitLogs = habitLogs,
                            routineLogs = routineLogs,
                            taskStats = taskStats,
                        )
                    } else {
                        val habitsCompletedByDate =
                            habitLogs.filter { it.isCompleted }.groupingBy { it.date }.eachCount()
                        val routinesCompletedByDate =
                            routineLogs.filter { it.isCompleted }.groupingBy { it.date }.eachCount()

                        fetchDates.map { date ->
                            ActivityDataPoint(
                                label = formatLabelForDate(date = date, timeFilter = timeFilter, isSingleDay = false),
                                habitsCompleted = habitsCompletedByDate[date] ?: 0,
                                routinesCompleted = routinesCompletedByDate[date] ?: 0,
                                tasksCompleted = taskStats.completedByDate[date] ?: 0,
                            )
                        }
                    }

                StatsState(
                    totalHabits = totalHabitTargets,
                    completedToday = completedHabits,
                    overallCompletionRate = habitRate,
                    currentBestStreak = habitStats.maxOfOrNull { it.currentStreak } ?: 0,
                    habitStats = habitStats,
                    todaysFocusSeconds = focusSeconds,
                    todaysPomodoroSessions = completedWorkSessions,
                    todaysCompletedRounds = completedRounds,
                    completedTasksToday = taskStats.completed,
                    totalTasks = taskStats.total,
                    taskCompletionRate = taskStats.completionRate,
                    completedRoutinesToday = routineStats.completed,
                    totalRoutines = routineStats.total,
                    routineCompletionRate = routineStats.completionRate,
                    timeFilter = timeFilter,
                    activityData = activityData,
                    graphType = graphType,
                    isLoading = false,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = StatsState(),
            )

        fun onTimeFilterChanged(filter: Int) {
            if (filter in 0..2) {
                timeFilterFlow.value = filter
            }
        }

        fun setGraphType(type: String) {
            viewModelScope.launch {
                appSettingsManager.setStatsGraphType(type)
            }
        }

        private fun getDateRangeForFilter(filter: Int): StatsDateRange =
            when (filter) {
                1 -> StatsDateRange(startDate = today.minusDays(6), endDate = today)
                2 -> StatsDateRange(startDate = today.minusDays(29), endDate = today)
                else -> StatsDateRange(startDate = today, endDate = today)
            }

        private fun buildTaskStats(
            tasks: List<Task>,
            range: StatsDateRange,
        ): TaskStatsData {
            val zone = ZoneId.systemDefault()
            val startMillis = range.startDate.atStartOfDay(zone).toInstant().toEpochMilli()
            val endMillis = range.endDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

            val completedInRange =
                tasks.count { task ->
                    task.isCompleted &&
                        task.completedAt?.let { completedAt ->
                            completedAt >= startMillis && completedAt < endMillis
                        } == true
                }

            val totalInRange = tasks.size
            val rate = if (totalInRange > 0) completedInRange.toFloat() / totalInRange else 0f
            val completedByDate =
                tasks
                    .mapNotNull { task ->
                        val completedAt = task.completedAt ?: return@mapNotNull null
                        if (!task.isCompleted) return@mapNotNull null
                        if (completedAt < startMillis || completedAt >= endMillis) return@mapNotNull null
                        completedAt.toLocalDate(zone)
                    }
                    .groupingBy { it }
                    .eachCount()

            val completedByHour =
                tasks
                    .mapNotNull { task ->
                        val completedAt = task.completedAt ?: return@mapNotNull null
                        if (!task.isCompleted) return@mapNotNull null
                        if (completedAt < startMillis || completedAt >= endMillis) return@mapNotNull null
                        completedAt.toLocalHour(zone)
                    }
                    .groupingBy { it }
                    .eachCount()

            return TaskStatsData(
                completed = completedInRange,
                total = totalInRange,
                completionRate = rate,
                completedByDate = completedByDate,
                completedByHour = completedByHour,
            )
        }

        private fun buildHourlyActivityData(
            habitLogs: List<com.habitao.domain.model.HabitLog>,
            routineLogs: List<RoutineLog>,
            taskStats: TaskStatsData,
        ): List<ActivityDataPoint> {
            val zone = ZoneId.systemDefault()
            val habitsByHour =
                habitLogs.countCompletedByHour(
                    zone = zone,
                    dateSelector = { it.date },
                    completedSelector = { it.isCompleted },
                    completedAtSelector = { it.completedAt ?: it.updatedAt },
                )
            val routinesByHour =
                routineLogs.countCompletedByHour(
                    zone = zone,
                    dateSelector = { it.date },
                    completedSelector = { it.isCompleted },
                    completedAtSelector = { it.completedAt ?: it.updatedAt },
                )

            return (0..23).map { hour ->
                ActivityDataPoint(
                    label = "%02d:00".format(hour),
                    habitsCompleted = habitsByHour[hour] ?: 0,
                    routinesCompleted = routinesByHour[hour] ?: 0,
                    tasksCompleted = taskStats.completedByHour[hour] ?: 0,
                )
            }
        }

        private fun <T> List<T>.countCompletedByHour(
            zone: ZoneId,
            dateSelector: (T) -> LocalDate,
            completedSelector: (T) -> Boolean,
            completedAtSelector: (T) -> Long,
        ): Map<Int, Int> =
            asSequence()
                .filter { completedSelector(it) && dateSelector(it) == today }
                .map { completedAtSelector(it).toLocalHour(zone) }
                .groupingBy { it }
                .eachCount()

        private fun formatLabelForDate(
            date: LocalDate,
            timeFilter: Int,
            isSingleDay: Boolean,
        ): String {
            if (isSingleDay) return "Today"

            return when (timeFilter) {
                1 -> date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                2 -> date.dayOfMonth.toString()
                else -> date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            }
        }

        private fun Long.toLocalDate(zoneId: ZoneId): LocalDate =
            Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()

        private fun Long.toLocalHour(zoneId: ZoneId): Int = Instant.ofEpochMilli(this).atZone(zoneId).hour

        private suspend fun loadStreakSafe(habitId: String): StreakInfo {
            return withContext(Dispatchers.IO) {
                habitRepository.calculateStreak(habitId)
                    .getOrElse { StreakInfo(0, 0, 0) }
            }
        }
    }
