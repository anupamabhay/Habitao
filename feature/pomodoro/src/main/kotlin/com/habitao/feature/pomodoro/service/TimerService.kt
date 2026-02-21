package com.habitao.feature.pomodoro.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.Manifest
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class TimerService : LifecycleService() {
    @Inject
    lateinit var timerStateHolder: TimerStateHolder

    @Inject
    lateinit var pomodoroRepository: PomodoroRepository

    private var timerJob: Job? = null
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var pomodoroPreferences: PomodoroPreferences
    private val notificationManager by lazy {
        getSystemService(NotificationManager::class.java)
    }

    // Cache PendingIntents to avoid recreating them every second (ANR prevention)
    private var pausePendingIntent: PendingIntent? = null
    private var resumePendingIntent: PendingIntent? = null
    private var stopPendingIntent: PendingIntent? = null

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        pomodoroPreferences = PomodoroPreferences(this)
        initPendingIntents()
    }

    private fun initPendingIntents() {
        val pauseIntent = Intent(this, TimerService::class.java).apply { action = ACTION_PAUSE }
        pausePendingIntent = PendingIntent.getService(
            this,
            REQUEST_CODE_PAUSE,
            pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val resumeIntent = Intent(this, TimerService::class.java).apply { action = ACTION_RESUME }
        resumePendingIntent = PendingIntent.getService(
            this,
            REQUEST_CODE_RESUME,
            resumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val stopIntent = Intent(this, TimerService::class.java).apply { action = ACTION_STOP }
        stopPendingIntent = PendingIntent.getService(
            this,
            REQUEST_CODE_STOP,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
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
                PomodoroType.WORK -> pomodoroPreferences.workDurationMinutes.toLong() * 60
                PomodoroType.SHORT_BREAK -> pomodoroPreferences.shortBreakDurationMinutes.toLong() * 60
                PomodoroType.LONG_BREAK -> pomodoroPreferences.longBreakDurationMinutes.toLong() * 60
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
            lifecycleScope.launch(Dispatchers.Default) {
                while (true) {
                    val remaining = ((endTimestamp - System.currentTimeMillis()) / 1000).coerceAtLeast(0L)
                    timerStateHolder.updateRemainingSeconds(remaining)
                    withContext(Dispatchers.Main) {
                        updateNotification(remaining)
                    }
                    if (remaining <= 0L) {
                        timerStateHolder.updateTimerState(TimerState.FINISHED)
                        withContext(Dispatchers.Main) {
                            handleTimerFinished()
                        }
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
        playCompletionFeedback()
        showCompletionNotification()
        advanceSessionType()
        timerStateHolder.updateTimerState(TimerState.IDLE)
        updateNotification(0L)
    }

    private fun playCompletionFeedback() {
        lifecycleScope.launch(Dispatchers.Default) {
            vibrateCompletionPulse()
            playDefaultCompletionSound()
        }
    }

    private fun vibrateCompletionPulse() {
        if (checkSelfPermission(Manifest.permission.VIBRATE) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val vibrator: Vibrator? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

        if (vibrator?.hasVibrator() != true) {
            return
        }

        val effect = VibrationEffect.createOneShot(COMPLETION_VIBRATION_DURATION_MS, VibrationEffect.DEFAULT_AMPLITUDE)
        vibrator.vibrate(effect)
    }

    private fun playDefaultCompletionSound() {
        try {
            val uri =
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    ?: return
            val ringtone = RingtoneManager.getRingtone(applicationContext, uri) ?: return
            ringtone.play()
        } catch (_: Throwable) {
            // Best-effort only: some devices/OS versions can throw or block playback.
        }
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
                workDurationSeconds = pomodoroPreferences.workDurationMinutes * 60,
                breakDurationSeconds = pomodoroPreferences.shortBreakDurationMinutes * 60,
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
                    if (newCount % pomodoroPreferences.sessionsBeforeLongBreak == 0) {
                        PomodoroType.LONG_BREAK
                    } else {
                        PomodoroType.SHORT_BREAK
                    }
                timerStateHolder.updateCurrentSessionType(nextType)
            }

            PomodoroType.SHORT_BREAK -> timerStateHolder.updateCurrentSessionType(PomodoroType.WORK)
            PomodoroType.LONG_BREAK -> {
                // Reset session counter after completing long break (start fresh cycle)
                timerStateHolder.updateCompletedWorkSessions(0)
                timerStateHolder.updateCurrentSessionType(PomodoroType.WORK)
            }
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
        // Use cached PendingIntents to avoid ANR from recreating them every second
        val isRunning = timerStateHolder.timerState.value == TimerState.RUNNING
        val actionPendingIntent = if (isRunning) pausePendingIntent else resumePendingIntent
        val actionTitle = if (isRunning) "Pause" else "Resume"
        val contentText = formatTime(remainingSeconds)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Pomodoro Timer")
            .setContentText(contentText)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .addAction(0, actionTitle, actionPendingIntent)
            .addAction(0, "Stop", stopPendingIntent)
            .build()
    }

    private fun showCompletionNotification() {
        val contentIntent =
            packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
                PendingIntent.getActivity(
                    this,
                    REQUEST_CODE_COMPLETE,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            }

        val notification =
            NotificationCompat.Builder(this, COMPLETE_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Pomodoro Complete")
                .setContentText("Session finished")
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText("Session finished")
                )
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVibrate(COMPLETION_VIBRATION_PATTERN)
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

            val completeChannel =
                NotificationChannel(
                    COMPLETE_CHANNEL_ID,
                    COMPLETE_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Pomodoro completion"
                    enableVibration(true)
                    vibrationPattern = COMPLETION_VIBRATION_PATTERN
                }
            notificationManager.createNotificationChannel(completeChannel)
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

        private const val COMPLETE_CHANNEL_ID = "pomodoro_complete"
        private const val COMPLETE_CHANNEL_NAME = "Pomodoro Complete"

        private const val NOTIFICATION_ID = 1001
        private const val NOTIFICATION_COMPLETE_ID = 1002
        private const val REQUEST_CODE_PAUSE = 2001
        private const val REQUEST_CODE_STOP = 2002
        private const val REQUEST_CODE_RESUME = 2003
        private const val REQUEST_CODE_COMPLETE = 2004

        private const val COMPLETION_VIBRATION_DURATION_MS = 250L
        private val COMPLETION_VIBRATION_PATTERN = longArrayOf(0L, COMPLETION_VIBRATION_DURATION_MS)
        private const val PREFS_NAME = "pomodoro_timer_prefs"
        private const val PREF_END_TIMESTAMP = "end_timestamp"
        private const val PREF_REMAINING_SECONDS = "remaining_seconds"
    }
}
