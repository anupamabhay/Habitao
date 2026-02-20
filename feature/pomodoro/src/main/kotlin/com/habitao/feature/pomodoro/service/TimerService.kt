package com.habitao.feature.pomodoro.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.habitao.domain.model.PomodoroSession
import com.habitao.domain.model.PomodoroType
import com.habitao.domain.repository.PomodoroRepository

import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TimerService : LifecycleService() {
    @Inject
    lateinit var timerStateHolder: TimerStateHolder

    @Inject
    lateinit var pomodoroRepository: PomodoroRepository

    private var timerJob: Job? = null
    private lateinit var sharedPreferences: SharedPreferences
    private val notificationManager by lazy {
        getSystemService(NotificationManager::class.java)
    }

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (intent?.action) {
            ACTION_START -> handleStart()
            ACTION_PAUSE -> handlePause()
            ACTION_RESUME -> handleResume()
            ACTION_STOP -> handleStop()
            ACTION_SKIP -> handleSkip()
        }
        return START_STICKY
    }

    private fun handleStart() {
        createNotificationChannel()
        if (timerStateHolder.timerState.value == TimerState.RUNNING) {
            return
        }
        val remaining = if (timerStateHolder.remainingSeconds.value > 0L) {
            timerStateHolder.remainingSeconds.value
        } else {
            when (timerStateHolder.currentSessionType.value) {
                PomodoroType.WORK -> DEFAULT_WORK_SECONDS
                PomodoroType.SHORT_BREAK -> DEFAULT_SHORT_BREAK_SECONDS
                PomodoroType.LONG_BREAK -> DEFAULT_LONG_BREAK_SECONDS
            }
        }
        timerStateHolder.updateTotalSeconds(remaining)
        timerStateHolder.updateRemainingSeconds(remaining)
        timerStateHolder.updateTimerState(TimerState.RUNNING)
        startForegroundWithNotification(remaining)
        startTimer(remaining)
    }

    private fun handlePause() {
        if (timerStateHolder.timerState.value != TimerState.RUNNING) {
            return
        }
        val remaining = timerStateHolder.remainingSeconds.value
        sharedPreferences.edit().putLong(PREF_REMAINING_SECONDS, remaining).apply()
        timerJob?.cancel()
        timerStateHolder.updateTimerState(TimerState.PAUSED)
        updateNotification(remaining)
    }

    private fun handleResume() {
        if (timerStateHolder.timerState.value != TimerState.PAUSED) {
            return
        }
        val remaining = timerStateHolder.remainingSeconds.value
        timerStateHolder.updateTimerState(TimerState.RUNNING)
        startTimer(remaining)
    }

    private fun handleStop() {
        timerJob?.cancel()
        val remaining = timerStateHolder.remainingSeconds.value
        val total = timerStateHolder.totalSeconds.value
        val elapsed = (total - remaining).coerceAtLeast(0L)
        saveSession(
            wasInterrupted = true,
            actualDurationSeconds = elapsed,
            completedAt = null,
        )
        clearTimerPrefs()
        timerStateHolder.reset()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun handleSkip() {
        timerJob?.cancel()
        advanceSessionType()
        timerStateHolder.updateRemainingSeconds(0L)
        timerStateHolder.updateTotalSeconds(0L)
        clearTimerPrefs()
        timerStateHolder.updateTimerState(TimerState.IDLE)
        updateNotification(0L)
    }

    private fun startTimer(initialRemainingSeconds: Long) {
        val endTimestamp = System.currentTimeMillis() + initialRemainingSeconds * 1000
        sharedPreferences.edit().putLong(PREF_END_TIMESTAMP, endTimestamp).apply()
        timerJob?.cancel()
        timerJob =
            lifecycleScope.launch {
                while (true) {
                    val remaining = ((endTimestamp - System.currentTimeMillis()) / 1000).coerceAtLeast(0L)
                    timerStateHolder.updateRemainingSeconds(remaining)
                    updateNotification(remaining)
                    if (remaining <= 0L) {
                        timerStateHolder.updateTimerState(TimerState.FINISHED)
                        handleTimerFinished()
                        break
                    }
                    delay(1000)
                }
            }
    }

    private fun handleTimerFinished() {
        val completedAt = System.currentTimeMillis()
        val total = timerStateHolder.totalSeconds.value
        saveSession(
            wasInterrupted = false,
            actualDurationSeconds = total,
            completedAt = completedAt,
        )
        clearTimerPrefs()
        showCompletionNotification()
        advanceSessionType()
        timerStateHolder.updateTimerState(TimerState.IDLE)
        updateNotification(0L)
    }

    private fun saveSession(
        wasInterrupted: Boolean,
        actualDurationSeconds: Long,
        completedAt: Long?,
    ) {
        val sessionType = timerStateHolder.currentSessionType.value
        val total = timerStateHolder.totalSeconds.value
        val startedAt = System.currentTimeMillis() - (total - timerStateHolder.remainingSeconds.value) * 1000
        val session =
            PomodoroSession(
                id = UUID.randomUUID().toString(),
                sessionType = sessionType,
                workDurationSeconds = DEFAULT_WORK_SECONDS.toInt(),
                breakDurationSeconds = DEFAULT_SHORT_BREAK_SECONDS.toInt(),
                startedAt = startedAt,
                completedAt = completedAt,
                wasInterrupted = wasInterrupted,
                actualDurationSeconds = actualDurationSeconds.toInt(),
                createdAt = System.currentTimeMillis(),
            )
        lifecycleScope.launch {
            pomodoroRepository.saveSession(session)
        }
    }

    private fun advanceSessionType() {
        val current = timerStateHolder.currentSessionType.value
        val completedWorkSessions = timerStateHolder.completedWorkSessions.value
        when (current) {
            PomodoroType.WORK -> {
                val newCount = completedWorkSessions + 1
                timerStateHolder.updateCompletedWorkSessions(newCount)
                val nextType =
                    if (newCount % SESSIONS_BEFORE_LONG_BREAK == 0) {
                        PomodoroType.LONG_BREAK
                    } else {
                        PomodoroType.SHORT_BREAK
                    }
                timerStateHolder.updateCurrentSessionType(nextType)
            }

            PomodoroType.SHORT_BREAK -> timerStateHolder.updateCurrentSessionType(PomodoroType.WORK)
            PomodoroType.LONG_BREAK -> timerStateHolder.updateCurrentSessionType(PomodoroType.WORK)
        }
    }

    private fun startForegroundWithNotification(remainingSeconds: Long) {
        val notification = buildNotification(remainingSeconds)
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
            } else {
                0
            },
        )
    }

    private fun updateNotification(remainingSeconds: Long) {
        val notification = buildNotification(remainingSeconds)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(remainingSeconds: Long): Notification {
        val pauseIntent =
            Intent(this, TimerService::class.java).apply {
                action =
                    if (timerStateHolder.timerState.value == TimerState.RUNNING) {
                        ACTION_PAUSE
                    } else {
                        ACTION_RESUME
                    }
            }
        val pausePendingIntent =
            PendingIntent.getService(
                this,
                REQUEST_CODE_PAUSE,
                pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val stopIntent =
            Intent(this, TimerService::class.java).apply {
                action = ACTION_STOP
            }
        val stopPendingIntent =
            PendingIntent.getService(
                this,
                REQUEST_CODE_STOP,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val pauseTitle =
            if (timerStateHolder.timerState.value == TimerState.RUNNING) {
                "Pause"
            } else {
                "Resume"
            }
        val contentText = formatTime(remainingSeconds)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Pomodoro Timer")
            .setContentText(contentText)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .addAction(0, pauseTitle, pausePendingIntent)
            .addAction(0, "Stop", stopPendingIntent)
            .build()
    }

    private fun showCompletionNotification() {
        val notification =
            NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Pomodoro Complete")
                .setContentText("Session finished")
                .setAutoCancel(true)
                .build()
        notificationManager.notify(NOTIFICATION_COMPLETE_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW,
                )
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun clearTimerPrefs() {
        sharedPreferences.edit()
            .remove(PREF_END_TIMESTAMP)
            .remove(PREF_REMAINING_SECONDS)
            .apply()
    }

    private fun formatTime(seconds: Long): String {
        return String.format("%02d:%02d", seconds / 60, seconds % 60)
    }

    companion object {
        const val ACTION_START = "com.habitao.feature.pomodoro.action.START"
        const val ACTION_PAUSE = "com.habitao.feature.pomodoro.action.PAUSE"
        const val ACTION_RESUME = "com.habitao.feature.pomodoro.action.RESUME"
        const val ACTION_STOP = "com.habitao.feature.pomodoro.action.STOP"
        const val ACTION_SKIP = "com.habitao.feature.pomodoro.action.SKIP"

        const val DEFAULT_WORK_SECONDS = 1500L
        const val DEFAULT_SHORT_BREAK_SECONDS = 300L
        const val DEFAULT_LONG_BREAK_SECONDS = 900L
        const val SESSIONS_BEFORE_LONG_BREAK = 4

        private const val CHANNEL_ID = "pomodoro_timer"
        private const val CHANNEL_NAME = "Pomodoro Timer"
        private const val NOTIFICATION_ID = 1001
        private const val NOTIFICATION_COMPLETE_ID = 1002
        private const val REQUEST_CODE_PAUSE = 2001
        private const val REQUEST_CODE_STOP = 2002
        private const val PREFS_NAME = "pomodoro_timer_prefs"
        private const val PREF_END_TIMESTAMP = "end_timestamp"
        private const val PREF_REMAINING_SECONDS = "remaining_seconds"
    }
}
