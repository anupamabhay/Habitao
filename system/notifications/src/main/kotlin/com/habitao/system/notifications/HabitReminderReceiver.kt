package com.habitao.system.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.habitao.core.datastore.AppSettingsRepository
import com.habitao.domain.model.DayOfWeek
import com.habitao.domain.model.FrequencyType
import com.habitao.system.notifications.NotificationConstants.EXTRA_FREQUENCY_TYPE
import com.habitao.system.notifications.NotificationConstants.EXTRA_HABIT_ID
import com.habitao.system.notifications.NotificationConstants.EXTRA_HABIT_TITLE
import com.habitao.system.notifications.NotificationConstants.EXTRA_REMINDER_HOUR
import com.habitao.system.notifications.NotificationConstants.EXTRA_REMINDER_MINUTE
import com.habitao.system.notifications.NotificationConstants.EXTRA_SCHEDULED_DAYS
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime

class HabitReminderReceiver : BroadcastReceiver(), KoinComponent {
    private val notificationHelper: NotificationHelper by inject()
    private val scheduler: HabitReminderScheduler by inject()
    private val appSettingsManager: AppSettingsRepository by inject()

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val habitId = intent.getStringExtra(EXTRA_HABIT_ID) ?: return
        val habitTitle = intent.getStringExtra(EXTRA_HABIT_TITLE) ?: "Habit"
        val hour = intent.getIntExtra(EXTRA_REMINDER_HOUR, 9)
        val minute = intent.getIntExtra(EXTRA_REMINDER_MINUTE, 0)
        val frequencyType =
            intent.getStringExtra(EXTRA_FREQUENCY_TYPE)
                ?.let { runCatching { FrequencyType.valueOf(it) }.getOrNull() }
                ?: FrequencyType.DAILY
        val scheduledDays =
            intent.getStringArrayListExtra(EXTRA_SCHEDULED_DAYS)
                ?.mapNotNull { runCatching { DayOfWeek.valueOf(it) }.getOrNull() }
                ?.toSet()
                ?: emptySet()

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Check if habit reminders are enabled in settings
                val settings = appSettingsManager.settings.first()
                if (!settings.habitRemindersEnabled) {
                    // Still reschedule so it fires again next time (toggle may be re-enabled)
                    scheduler.scheduleReminder(
                        habitId = habitId,
                        habitTitle = habitTitle,
                        time = LocalTime(hour = hour, minute = minute),
                        frequencyType = frequencyType,
                        scheduledDays = scheduledDays,
                    )
                    return@launch
                }

                val hasPermission =
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                        notificationHelper.hasNotificationPermission()
                if (hasPermission) {
                    notificationHelper.showReminder(habitId, habitTitle)
                }

                scheduler.scheduleReminder(
                    habitId = habitId,
                    habitTitle = habitTitle,
                    time = LocalTime(hour = hour, minute = minute),
                    frequencyType = frequencyType,
                    scheduledDays = scheduledDays,
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
