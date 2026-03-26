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
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter

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
    private val center = UNUserNotificationCenter.currentNotificationCenter()
    private val notificationId = "pomodoro_timer_complete"

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
        scheduleCompletionNotification(timerStateHolder.remainingSeconds.value)
        scheduleCountdown()
    }

    override fun pause() {
        timerJob?.cancel()
        cancelCompletionNotification()
        timerStateHolder.updateTimerState(TimerState.PAUSED)
    }

    override fun resume() {
        timerStateHolder.updateTimerState(TimerState.RUNNING)
        scheduleCompletionNotification(timerStateHolder.remainingSeconds.value)
        scheduleCountdown()
    }

    override fun stop() {
        timerJob?.cancel()
        cancelCompletionNotification()
        timerStateHolder.resetTimerState()
    }

    override fun skip() {
        timerJob?.cancel()
        cancelCompletionNotification()
        timerStateHolder.updateTimerState(TimerState.FINISHED)
    }

    override fun adjustTime(deltaSeconds: Long) {
        val current = timerStateHolder.remainingSeconds.value
        val adjusted = (current + deltaSeconds).coerceAtLeast(0L)
        timerStateHolder.updateRemainingSeconds(adjusted)
        // Reschedule notification with updated time
        if (timerStateHolder.timerState.value == TimerState.RUNNING) {
            scheduleCompletionNotification(adjusted)
        }
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

    private fun scheduleCompletionNotification(remainingSeconds: Long) {
        cancelCompletionNotification()
        if (remainingSeconds <= 0L) return

        val sessionType = timerStateHolder.currentSessionType.value
        val (title, body) = when (sessionType) {
            PomodoroType.WORK -> "Focus session complete!" to "Great work! Time for a break."
            PomodoroType.SHORT_BREAK -> "Short break over!" to "Ready to focus again?"
            PomodoroType.LONG_BREAK -> "Long break over!" to "Feeling refreshed? Let's get back to work."
        }

        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(body)
            setSound(UNNotificationSound.defaultSound())
        }
        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
            remainingSeconds.toDouble(), repeats = false
        )
        val request = UNNotificationRequest.requestWithIdentifier(notificationId, content, trigger)
        center.addNotificationRequest(request, null)
    }

    private fun cancelCompletionNotification() {
        center.removePendingNotificationRequestsWithIdentifiers(listOf(notificationId))
    }
}
