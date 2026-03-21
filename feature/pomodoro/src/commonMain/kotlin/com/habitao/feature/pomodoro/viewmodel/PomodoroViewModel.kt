package com.habitao.feature.pomodoro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habitao.domain.model.PomodoroType
import com.habitao.domain.repository.HabitRepository
import com.habitao.domain.repository.PomodoroRepository
import com.habitao.domain.repository.TaskRepository
import com.habitao.feature.pomodoro.preferences.PomodoroPreferencesSource
import com.habitao.feature.pomodoro.service.TimerState
import com.habitao.feature.pomodoro.service.TimerStateHolder
import com.habitao.feature.pomodoro.timer.TimerController
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

enum class FocusLinkType {
    TASK,
    HABIT,
}

data class FocusLinkOption(
    val id: String,
    val title: String,
    val type: FocusLinkType,
)

data class PomodoroState(
    val timerState: TimerState = TimerState.IDLE,
    val remainingSeconds: Long = 1500L,
    val totalSeconds: Long = 1500L,
    val currentSessionType: PomodoroType = PomodoroType.WORK,
    val completedWorkSessions: Int = 0,
    val totalCompletedWorkSessions: Int = 0,
    val sessionsBeforeLongBreak: Int = 4,
    val totalSessions: Int = 5,
    val todaysFocusSeconds: Int = 0,
    val todaysSessions: Int = 0,
    val todaysRounds: Int = 0,
    val activeTaskOptions: List<FocusLinkOption> = emptyList(),
    val activeHabitOptions: List<FocusLinkOption> = emptyList(),
    val selectedFocusOption: FocusLinkOption? = null,
)

sealed class PomodoroIntent {
    data object StartTimer : PomodoroIntent()

    data object PauseTimer : PomodoroIntent()

    data object ResumeTimer : PomodoroIntent()

    data object StopTimer : PomodoroIntent()

    data object SkipToNext : PomodoroIntent()

    data class AdjustTime(val deltaSeconds: Long) : PomodoroIntent()

    data class LinkTask(val taskId: String) : PomodoroIntent()

    data class LinkHabit(val habitId: String) : PomodoroIntent()

    data object ClearLinkedFocus : PomodoroIntent()
}

class PomodoroViewModel
    constructor(
        private val timerStateHolder: TimerStateHolder,
        private val pomodoroRepository: PomodoroRepository,
        private val taskRepository: TaskRepository,
        private val habitRepository: HabitRepository,
        private val preferencesSource: PomodoroPreferencesSource,
        private val timerController: TimerController,
    ) : ViewModel() {
        private val sessionsFlow =
            pomodoroRepository.observeSessionsForDate(
                Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
            )
                .map { result -> result.getOrElse { emptyList() } }
                .catch { emit(emptyList()) }

        private val todaysFocusSecondsFlow =
            sessionsFlow.map { sessions ->
                sessions
                    .filter { session -> session.sessionType == PomodoroType.WORK }
                    .sumOf { session -> session.actualDurationSeconds ?: 0 }
            }

        private val todaysSessionsFlow =
            sessionsFlow.map { sessions ->
                sessions.count { session ->
                    session.sessionType == PomodoroType.WORK && (session.actualDurationSeconds ?: 0) > 0
                }
            }

        private val tasksFlow =
            taskRepository.observeAllTasks()
                .map { result -> result.getOrElse { emptyList() } }
                .catch { emit(emptyList()) }

        private val habitsFlow =
            habitRepository.observeAllHabits()
                .map { result -> result.getOrElse { emptyList() } }
                .catch { emit(emptyList()) }

        private val focusSelectionFlow =
            combine(
                tasksFlow,
                habitsFlow,
                timerStateHolder.linkedTaskId,
                timerStateHolder.linkedHabitId,
            ) { tasks, habits, linkedTaskId, linkedHabitId ->
                val activeTaskOptions =
                    tasks
                        .filter { task -> !task.isCompleted && task.deletedAt == null }
                        .sortedBy { task -> task.title.lowercase() }
                        .map { task -> FocusLinkOption(id = task.id, title = task.title, type = FocusLinkType.TASK) }

                val activeHabitOptions =
                    habits
                        .filterNot { habit -> habit.isArchived }
                        .sortedBy { habit -> habit.title.lowercase() }
                        .map { habit -> FocusLinkOption(id = habit.id, title = habit.title, type = FocusLinkType.HABIT) }

                val selectedFocusOption =
                    when {
                        linkedTaskId != null ->
                            tasks.firstOrNull { it.id == linkedTaskId }?.let {
                                FocusLinkOption(it.id, it.title, FocusLinkType.TASK)
                            } ?: FocusLinkOption(linkedTaskId, "Selected task", FocusLinkType.TASK)

                        linkedHabitId != null ->
                            habits.firstOrNull { it.id == linkedHabitId }?.let {
                                FocusLinkOption(it.id, it.title, FocusLinkType.HABIT)
                            } ?: FocusLinkOption(linkedHabitId, "Selected habit", FocusLinkType.HABIT)

                        else -> null
                    }

                FocusSelectionSnapshot(activeTaskOptions, activeHabitOptions, selectedFocusOption)
            }

        private val sessionsCountFlow =
            combine(
                timerStateHolder.completedWorkSessions,
                timerStateHolder.totalCompletedWorkSessions,
            ) { cycle, total -> Pair(cycle, total) }

        private val timerCombinedFlow =
            combine(
                timerStateHolder.timerState,
                timerStateHolder.remainingSeconds,
                timerStateHolder.totalSeconds,
                timerStateHolder.currentSessionType,
                sessionsCountFlow,
            ) { timerState, remainingSeconds, totalSeconds, currentSessionType, counts ->
                TimerSnapshot(timerState, remainingSeconds, totalSeconds, currentSessionType, counts.first, counts.second)
            }

        val state: StateFlow<PomodoroState> =
            combine(
                timerCombinedFlow,
                todaysFocusSecondsFlow,
                todaysSessionsFlow,
                preferencesSource.observeChanges(),
                focusSelectionFlow,
            ) { timer, todaysFocusSeconds, todaysSessions, _, focusSelection ->
                val defaultTotalSeconds =
                    when (timer.currentSessionType) {
                        PomodoroType.WORK -> preferencesSource.workDurationMinutes.toLong() * 60
                        PomodoroType.SHORT_BREAK -> preferencesSource.shortBreakDurationMinutes.toLong() * 60
                        PomodoroType.LONG_BREAK -> preferencesSource.longBreakDurationMinutes.toLong() * 60
                    }
                val safeTotalSeconds = if (timer.totalSeconds > 0L) timer.totalSeconds else defaultTotalSeconds
                val safeRemainingSeconds = if (timer.remainingSeconds > 0L) timer.remainingSeconds else safeTotalSeconds

                PomodoroState(
                    timerState = timer.timerState,
                    remainingSeconds = safeRemainingSeconds,
                    totalSeconds = safeTotalSeconds,
                    currentSessionType = timer.currentSessionType,
                    completedWorkSessions = timer.completedWorkSessions,
                    totalCompletedWorkSessions = timer.totalCompletedWorkSessions,
                    sessionsBeforeLongBreak = preferencesSource.sessionsBeforeLongBreak,
                    totalSessions = preferencesSource.totalSessions,
                    todaysFocusSeconds = todaysFocusSeconds,
                    todaysSessions = todaysSessions,
                    todaysRounds = preferencesSource.getTodaysRounds(),
                    activeTaskOptions = focusSelection.activeTaskOptions,
                    activeHabitOptions = focusSelection.activeHabitOptions,
                    selectedFocusOption = focusSelection.selectedFocusOption,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = PomodoroState(),
            )

        private data class FocusSelectionSnapshot(
            val activeTaskOptions: List<FocusLinkOption>,
            val activeHabitOptions: List<FocusLinkOption>,
            val selectedFocusOption: FocusLinkOption?,
        )

        private data class TimerSnapshot(
            val timerState: TimerState,
            val remainingSeconds: Long,
            val totalSeconds: Long,
            val currentSessionType: PomodoroType,
            val completedWorkSessions: Int,
            val totalCompletedWorkSessions: Int,
        )

        fun processIntent(intent: PomodoroIntent) {
            when (intent) {
                PomodoroIntent.StartTimer -> timerController.start()
                PomodoroIntent.PauseTimer -> timerController.pause()
                PomodoroIntent.ResumeTimer -> timerController.resume()
                PomodoroIntent.StopTimer -> timerController.stop()
                PomodoroIntent.SkipToNext -> timerController.skip()
                is PomodoroIntent.AdjustTime -> timerController.adjustTime(intent.deltaSeconds)
                is PomodoroIntent.LinkTask -> timerStateHolder.linkTask(intent.taskId)
                is PomodoroIntent.LinkHabit -> timerStateHolder.linkHabit(intent.habitId)
                PomodoroIntent.ClearLinkedFocus -> timerStateHolder.clearLinkedFocus()
            }
        }
    }
