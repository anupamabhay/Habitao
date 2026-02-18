package com.habitao.system.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.habitao.domain.repository.HabitRepository
import com.habitao.system.notifications.NotificationConstants.ACTION_HABIT_REMINDER
import com.habitao.system.notifications.NotificationConstants.EXTRA_HABIT_ID
import com.habitao.system.notifications.NotificationConstants.EXTRA_HABIT_TITLE
import com.habitao.system.notifications.NotificationConstants.EXTRA_REMINDER_HOUR
import com.habitao.system.notifications.NotificationConstants.EXTRA_REMINDER_MINUTE
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

class HabitReminderScheduler
    @Inject
    constructor(
        @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
        private val alarmManager: AlarmManager,
        private val habitRepository: HabitRepository,
    ) {
        fun scheduleReminder(
            habitId: String,
            habitTitle: String,
            time: LocalTime,
        ) {
            val triggerAt = calculateNextTrigger(time)
            val pendingIntent = buildReminderPendingIntent(habitId, habitTitle, time)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAt,
                        pendingIntent,
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAt,
                        pendingIntent,
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent,
                )
            }
        }

        fun cancelReminder(habitId: String) {
            val pendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    habitId.hashCode(),
                    Intent(context, HabitReminderReceiver::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            alarmManager.cancel(pendingIntent)
        }

        suspend fun rescheduleAllReminders() {
            val habits = habitRepository.getAllHabits().getOrElse { emptyList() }
            habits.filter { it.reminderEnabled && it.reminderTime != null }
                .forEach { habit ->
                    scheduleReminder(habit.id, habit.title, habit.reminderTime!!)
                }
        }

        private fun buildReminderPendingIntent(
            habitId: String,
            habitTitle: String,
            time: LocalTime,
        ): PendingIntent {
            val intent =
                Intent(context, HabitReminderReceiver::class.java).apply {
                    action = ACTION_HABIT_REMINDER
                    putExtra(EXTRA_HABIT_ID, habitId)
                    putExtra(EXTRA_HABIT_TITLE, habitTitle)
                    putExtra(EXTRA_REMINDER_HOUR, time.hour)
                    putExtra(EXTRA_REMINDER_MINUTE, time.minute)
                }
            return PendingIntent.getBroadcast(
                context,
                habitId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun calculateNextTrigger(time: LocalTime): Long {
            val now = LocalDateTime.now()
            var next = now.withHour(time.hour).withMinute(time.minute).withSecond(0).withNano(0)
            if (!next.isAfter(now)) {
                next = next.plusDays(1)
            }
            return next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
    }
