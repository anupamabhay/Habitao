package com.habitao.feature.pomodoro.viewmodel

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habitao.domain.model.PomodoroType
import com.habitao.domain.repository.PomodoroRepository
import com.habitao.feature.pomodoro.service.PomodoroPreferences
import com.habitao.feature.pomodoro.service.TimerService
import com.habitao.feature.pomodoro.service.TimerState
import com.habitao.feature.pomodoro.service.TimerStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
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
    val totalCompletedWorkSessions: Int = 0,
    val sessionsBeforeLongBreak: Int = 4,
    val totalSessions: Int = 5,
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
        private val pomodoroPreferences = PomodoroPreferences(context)

        private val sessionsFlow =
            pomodoroRepository.observeSessionsForDate(LocalDate.now())
                .map { result -> result.getOrElse { emptyList() } }
                .catch { emit(emptyList()) }

        private val todaysFocusMinutesFlow =
            sessionsFlow.map { sessions ->
                val totalSeconds =
                    sessions
                        .filter { session ->
                            session.sessionType == PomodoroType.WORK
                        }
                        .sumOf { session ->
                            session.actualDurationSeconds ?: 0
                        }
                totalSeconds / 60
            }

        private val todaysSessionsFlow =
            sessionsFlow.map { sessions ->
                sessions.count { session ->
                    session.sessionType == PomodoroType.WORK && (session.actualDurationSeconds ?: 0) > 0
                }
            }

        private val sessionsCountFlow = combine(
            timerStateHolder.completedWorkSessions,
            timerStateHolder.totalCompletedWorkSessions
        ) { cycle, total -> Pair(cycle, total) }

        private val timerCombinedFlow =
            combine(
                timerStateHolder.timerState,
                timerStateHolder.remainingSeconds,
                timerStateHolder.totalSeconds,
                timerStateHolder.currentSessionType,
                sessionsCountFlow,
            ) { timerState, remainingSeconds, totalSeconds, currentSessionType, counts ->
                TimerSnapshot(
                    timerState = timerState,
                    remainingSeconds = remainingSeconds,
                    totalSeconds = totalSeconds,
                    currentSessionType = currentSessionType,
                    completedWorkSessions = counts.first,
                    totalCompletedWorkSessions = counts.second,
                )
            }

        private val preferencesUpdateFlow = callbackFlow {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
                trySend(Unit)
            }
            pomodoroPreferences.sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
            trySend(Unit)
            awaitClose {
                pomodoroPreferences.sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }

        val state: StateFlow<PomodoroState> =
            combine(
                timerCombinedFlow,
                todaysFocusMinutesFlow,
                todaysSessionsFlow,
                preferencesUpdateFlow,
            ) { timer, todaysFocusMinutes, todaysSessions, _ ->
                val defaultTotalSeconds =
                    when (timer.currentSessionType) {
                        PomodoroType.WORK -> pomodoroPreferences.workDurationMinutes.toLong() * 60
                        PomodoroType.SHORT_BREAK -> pomodoroPreferences.shortBreakDurationMinutes.toLong() * 60
                        PomodoroType.LONG_BREAK -> pomodoroPreferences.longBreakDurationMinutes.toLong() * 60
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
                    totalCompletedWorkSessions = timer.totalCompletedWorkSessions,
                    sessionsBeforeLongBreak = pomodoroPreferences.sessionsBeforeLongBreak,
                    totalSessions = pomodoroPreferences.totalSessions,
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
            val totalCompletedWorkSessions: Int,
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
