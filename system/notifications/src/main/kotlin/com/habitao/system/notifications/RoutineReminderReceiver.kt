package com.habitao.system.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
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
import com.habitao.domain.model.DayOfWeek
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RoutineReminderReceiver : BroadcastReceiver(), KoinComponent {
    private val scheduler: RoutineReminderScheduler by inject()
    private val notificationHelper: NotificationHelper by inject()

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
        val startDateEpoch = intent.getLongExtra(EXTRA_START_DATE, Clock.System.now().toEpochMilliseconds())
        val startDate = Instant.fromEpochMilliseconds(startDateEpoch).toLocalDateTime(TimeZone.currentSystemDefault()).date

        val scheduledDays =
            intent.getStringArrayListExtra(EXTRA_SCHEDULED_DAYS)
                ?.mapNotNull { runCatching { DayOfWeek.valueOf(it) }.getOrNull() }
                ?.toSet()
                ?: emptySet()

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val hasPermission =
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                        notificationHelper.hasNotificationPermission()

                if (hasPermission) {
                    notificationHelper.showRoutineReminder(routineId, routineTitle)
                }

                // Re-schedule for next time
                scheduler.scheduleReminder(
                    routineId = routineId,
                    routineTitle = routineTitle,
                    time = LocalTime(hour = hour, minute = minute),
                    repeatPattern = repeatPattern,
                    repeatDays = scheduledDays,
                    customInterval = customInterval.coerceAtLeast(1),
                    startDate = startDate,
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
