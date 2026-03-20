package com.habitao.feature.pomodoro.service

import com.habitao.domain.model.PomodoroType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class TimerState {
    IDLE,
    RUNNING,
    PAUSED,
    FINISHED,
}

class TimerStateHolder
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

        private val _totalCompletedWorkSessions = MutableStateFlow(0)
        val totalCompletedWorkSessions: StateFlow<Int> = _totalCompletedWorkSessions.asStateFlow()

        private val _linkedTaskId = MutableStateFlow<String?>(null)
        val linkedTaskId: StateFlow<String?> = _linkedTaskId.asStateFlow()

        private val _linkedHabitId = MutableStateFlow<String?>(null)
        val linkedHabitId: StateFlow<String?> = _linkedHabitId.asStateFlow()

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

        fun updateTotalCompletedWorkSessions(count: Int) {
            _totalCompletedWorkSessions.value = count
        }

        fun linkTask(taskId: String) {
            _linkedTaskId.value = taskId
            _linkedHabitId.value = null
        }

        fun linkHabit(habitId: String) {
            _linkedHabitId.value = habitId
            _linkedTaskId.value = null
        }

        fun clearLinkedFocus() {
            _linkedTaskId.value = null
            _linkedHabitId.value = null
        }

        fun resetTimerState() {
            _timerState.value = TimerState.IDLE
            _remainingSeconds.value = 0L
            _totalSeconds.value = 0L
            _currentSessionType.value = PomodoroType.WORK
            _completedWorkSessions.value = 0
            _totalCompletedWorkSessions.value = 0
        }
    }
