package com.habitao.feature.pomodoro.service

import com.habitao.domain.model.PomodoroType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class TimerState {
    IDLE,
    RUNNING,
    PAUSED,
    FINISHED,
}

@Singleton
class TimerStateHolder
    @Inject
    constructor() {
        private val _timerState = MutableStateFlow(TimerState.IDLE)
        val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

        private val _remainingSeconds = MutableStateFlow(0L)
        val remainingSeconds: StateFlow<Long> = _remainingSeconds.asStateFlow()

        private val _totalSeconds = MutableStateFlow(0L)
        val totalSeconds: StateFlow<Long> = _totalSeconds.asStateFlow()

        private val _currentSessionType = MutableStateFlow(PomodoroType.WORK)
        val currentSessionType: StateFlow<PomodoroType> = _currentSessionType.asStateFlow()

        private val _completedWorkSessions = MutableStateFlow(0)
        val completedWorkSessions: StateFlow<Int> = _completedWorkSessions.asStateFlow()

        fun updateTimerState(state: TimerState) {
            _timerState.value = state
        }

        fun updateRemainingSeconds(seconds: Long) {
            _remainingSeconds.value = seconds
        }

        fun updateTotalSeconds(seconds: Long) {
            _totalSeconds.value = seconds
        }

        fun updateCurrentSessionType(type: PomodoroType) {
            _currentSessionType.value = type
        }

        fun updateCompletedWorkSessions(count: Int) {
            _completedWorkSessions.value = count
        }

        fun reset() {
            _timerState.value = TimerState.IDLE
            _remainingSeconds.value = 0L
            _totalSeconds.value = 0L
            _currentSessionType.value = PomodoroType.WORK
            _completedWorkSessions.value = 0
        }
    }
