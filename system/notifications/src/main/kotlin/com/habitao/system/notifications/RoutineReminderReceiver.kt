package com.habitao.system.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.habitao.core.datastore.AppSettingsManager
import com.habitao.domain.model.RepeatPattern
import com.habitao.system.notifications.NotificationConstants.ACTION_ROUTINE_REMINDER
import com.habitao.system.notifications.NotificationConstants.EXTRA_CUSTOM_INTERVAL
import com.habitao.system.notifications.NotificationConstants.EXTRA_REMINDER_HOUR
import com.habitao.system.notifications.NotificationConstants.EXTRA_REMINDER_MINUTE
import com.habitao.system.notifications.NotificationConstants.EXTRA_REPEAT_PATTERN
import com.habitao.system.notifications.NotificationConstants.EXTRA_ROUTINE_ID
import com.habitao.system.notifications.NotificationConstants.EXTRA_ROUTINE_TITLE
import com.habitao.system.notifications.NotificationConstants.EXTRA_SCHEDULED_DAYS
import com.habitao.system.notifications.NotificationConstants.EXTRA_START_DATE
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

@AndroidEntryPoint
class RoutineReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var appSettingsManager: AppSettingsManager

    @Inject lateinit var scheduler: RoutineReminderScheduler

    @Inject lateinit var notificationHelper: NotificationHelper

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != ACTION_ROUTINE_REMINDER) return

        val routineId = intent.getStringExtra(EXTRA_ROUTINE_ID) ?: return
        val routineTitle = intent.getStringExtra(EXTRA_ROUTINE_TITLE) ?: "Routine"
        val hour = intent.getIntExtra(EXTRA_REMINDER_HOUR, 9)
        val minute = intent.getIntExtra(EXTRA_REMINDER_MINUTE, 0)

        val repeatPatternStr = intent.getStringExtra(EXTRA_REPEAT_PATTERN)
        val repeatPattern =
            repeatPatternStr?.let {
                runCatching { RepeatPattern.valueOf(it) }.getOrNull()
            } ?: RepeatPattern.DAILY
        val customInterval = intent.getIntExtra(EXTRA_CUSTOM_INTERVAL, 1)
        val startDateEpoch = intent.getLongExtra(EXTRA_START_DATE, LocalDate.now().toEpochDay())
        val startDate = LocalDate.ofEpochDay(startDateEpoch)

        val scheduledDays =
            intent.getStringArrayListExtra(EXTRA_SCHEDULED_DAYS)
                ?.mapNotNull { runCatching { DayOfWeek.valueOf(it) }.getOrNull() }
                ?.toSet()
                ?: emptySet()

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Show notification immediately
                notificationHelper.showRoutineReminder(routineId, routineTitle)

                // Re-schedule for next time
                scheduler.scheduleReminder(
                    routineId = routineId,
                    routineTitle = routineTitle,
                    time = LocalTime.of(hour, minute),
                    repeatPattern = repeatPattern,
                    repeatDays = scheduledDays,
                    customInterval = customInterval,
                    startDate = startDate,
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
