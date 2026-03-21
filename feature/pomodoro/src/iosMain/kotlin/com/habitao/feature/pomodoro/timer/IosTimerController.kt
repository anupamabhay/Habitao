package com.habitao.feature.pomodoro.timer

import com.habitao.domain.model.PomodoroType
import com.habitao.feature.pomodoro.preferences.PomodoroPreferencesSource
import com.habitao.feature.pomodoro.service.TimerState
import com.habitao.feature.pomodoro.service.TimerStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * iOS implementation of [TimerController] that runs the countdown timer
 * in a coroutine scope (no ForegroundService available on iOS).
 */
class IosTimerController(
    private val timerStateHolder: TimerStateHolder,
    private val preferencesSource: PomodoroPreferencesSource,
) : TimerController {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timerJob: Job? = null

    override fun start() {
        // Initialize timer if starting from scratch
        if (timerStateHolder.remainingSeconds.value <= 0L) {
            val defaultMinutes = when (timerStateHolder.currentSessionType.value) {
                PomodoroType.WORK -> preferencesSource.workDurationMinutes
                PomodoroType.SHORT_BREAK -> preferencesSource.shortBreakDurationMinutes
                PomodoroType.LONG_BREAK -> preferencesSource.longBreakDurationMinutes
            }
            val totalSeconds = defaultMinutes * 60L
            timerStateHolder.updateTotalSeconds(totalSeconds)
            timerStateHolder.updateRemainingSeconds(totalSeconds)
        }

        timerStateHolder.updateTimerState(TimerState.RUNNING)
        scheduleCountdown()
    }

    override fun pause() {
        timerJob?.cancel()
        timerStateHolder.updateTimerState(TimerState.PAUSED)
    }

    override fun resume() {
        timerStateHolder.updateTimerState(TimerState.RUNNING)
        scheduleCountdown()
    }

    override fun stop() {
        timerJob?.cancel()
        timerStateHolder.resetTimerState()
    }

    override fun skip() {
        timerJob?.cancel()
        timerStateHolder.updateTimerState(TimerState.FINISHED)
    }

    override fun adjustTime(deltaSeconds: Long) {
        val current = timerStateHolder.remainingSeconds.value
        val adjusted = (current + deltaSeconds).coerceAtLeast(0L)
        timerStateHolder.updateRemainingSeconds(adjusted)
    }

    private fun scheduleCountdown() {
        timerJob?.cancel()
        timerJob =
            scope.launch {
                while (timerStateHolder.timerState.value == TimerState.RUNNING) {
                    delay(1_000L)
                    val remaining = timerStateHolder.remainingSeconds.value
                    if (remaining <= 0L) {
                        timerStateHolder.updateTimerState(TimerState.FINISHED)
                        break
                    }
                    timerStateHolder.updateRemainingSeconds(remaining - 1L)
                }
            }
    }
}
