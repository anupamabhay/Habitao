package com.habitao.feature.pomodoro.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habitao.domain.model.PomodoroType
import com.habitao.domain.repository.PomodoroRepository
import com.habitao.feature.pomodoro.service.TimerService
import com.habitao.feature.pomodoro.service.TimerState
import com.habitao.feature.pomodoro.service.TimerStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class PomodoroState(
    val timerState: TimerState = TimerState.IDLE,
    val remainingSeconds: Long = 1500L,
    val totalSeconds: Long = 1500L,
    val currentSessionType: PomodoroType = PomodoroType.WORK,
    val completedWorkSessions: Int = 0,
    val sessionsBeforeLongBreak: Int = 4,
    val todaysFocusMinutes: Int = 0,
    val todaysSessions: Int = 0,
)

sealed class PomodoroIntent {
    data object StartTimer : PomodoroIntent()

    data object PauseTimer : PomodoroIntent()

    data object ResumeTimer : PomodoroIntent()

    data object StopTimer : PomodoroIntent()

    data object SkipToNext : PomodoroIntent()
}

@HiltViewModel
class PomodoroViewModel
    @Inject
    constructor(
        private val timerStateHolder: TimerStateHolder,
        private val pomodoroRepository: PomodoroRepository,
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        private val sessionsFlow =
            pomodoroRepository.observeSessionsForDate(LocalDate.now())
                .map { result -> result.getOrElse { emptyList() } }
                .catch { emit(emptyList()) }

        private val todaysFocusMinutesFlow =
            sessionsFlow.map { sessions ->
                val totalSeconds =
                    sessions
                        .filter { session ->
                            session.sessionType == PomodoroType.WORK && !session.wasInterrupted
                        }
                        .sumOf { session ->
                            session.actualDurationSeconds ?: session.workDurationSeconds
                        }
                totalSeconds / 60
            }

        private val todaysSessionsFlow =
            sessionsFlow.map { sessions ->
                sessions.count { session ->
                    session.sessionType == PomodoroType.WORK && !session.wasInterrupted
                }
            }

        private val timerCombinedFlow =
            combine(
                timerStateHolder.timerState,
                timerStateHolder.remainingSeconds,
                timerStateHolder.totalSeconds,
                timerStateHolder.currentSessionType,
                timerStateHolder.completedWorkSessions,
            ) { timerState, remainingSeconds, totalSeconds, currentSessionType, completedWorkSessions ->
                TimerSnapshot(
                    timerState = timerState,
                    remainingSeconds = remainingSeconds,
                    totalSeconds = totalSeconds,
                    currentSessionType = currentSessionType,
                    completedWorkSessions = completedWorkSessions,
                )
            }

        val state: StateFlow<PomodoroState> =
            combine(
                timerCombinedFlow,
                todaysFocusMinutesFlow,
                todaysSessionsFlow,
            ) { timer, todaysFocusMinutes, todaysSessions ->
                val defaultTotalSeconds =
                    when (timer.currentSessionType) {
                        PomodoroType.WORK -> TimerService.DEFAULT_WORK_SECONDS
                        PomodoroType.SHORT_BREAK -> TimerService.DEFAULT_SHORT_BREAK_SECONDS
                        PomodoroType.LONG_BREAK -> TimerService.DEFAULT_LONG_BREAK_SECONDS
                    }
                val safeTotalSeconds =
                    if (timer.totalSeconds > 0L) timer.totalSeconds else defaultTotalSeconds
                val safeRemainingSeconds =
                    if (timer.remainingSeconds > 0L) timer.remainingSeconds else safeTotalSeconds

                PomodoroState(
                    timerState = timer.timerState,
                    remainingSeconds = safeRemainingSeconds,
                    totalSeconds = safeTotalSeconds,
                    currentSessionType = timer.currentSessionType,
                    completedWorkSessions = timer.completedWorkSessions,
                    sessionsBeforeLongBreak = TimerService.SESSIONS_BEFORE_LONG_BREAK,
                    todaysFocusMinutes = todaysFocusMinutes,
                    todaysSessions = todaysSessions,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = PomodoroState(),
            )

        private data class TimerSnapshot(
            val timerState: TimerState,
            val remainingSeconds: Long,
            val totalSeconds: Long,
            val currentSessionType: PomodoroType,
            val completedWorkSessions: Int,
        )

        fun processIntent(intent: PomodoroIntent) {
            when (intent) {
                PomodoroIntent.StartTimer -> sendServiceAction(TimerService.ACTION_START)
                PomodoroIntent.PauseTimer -> sendServiceAction(TimerService.ACTION_PAUSE)
                PomodoroIntent.ResumeTimer -> sendServiceAction(TimerService.ACTION_RESUME)
                PomodoroIntent.StopTimer -> sendServiceAction(TimerService.ACTION_STOP)
                PomodoroIntent.SkipToNext -> sendServiceAction(TimerService.ACTION_SKIP)
            }
        }

        private fun sendServiceAction(action: String) {
            val intent =
                Intent(context, TimerService::class.java).apply {
                    this.action = action
                }
            context.startService(intent)
        }
    }
