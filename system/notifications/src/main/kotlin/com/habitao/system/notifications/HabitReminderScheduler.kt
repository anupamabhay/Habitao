package com.habitao.system.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.habitao.domain.model.DayOfWeek
import com.habitao.domain.model.FrequencyType
import com.habitao.domain.model.toDomainDay
import com.habitao.domain.notification.HabitScheduler
import com.habitao.domain.repository.HabitRepository
import com.habitao.system.notifications.NotificationConstants.ACTION_HABIT_REMINDER
import com.habitao.system.notifications.NotificationConstants.EXTRA_FREQUENCY_TYPE
import com.habitao.system.notifications.NotificationConstants.EXTRA_HABIT_ID
import com.habitao.system.notifications.NotificationConstants.EXTRA_HABIT_TITLE
import com.habitao.system.notifications.NotificationConstants.EXTRA_REMINDER_HOUR
import com.habitao.system.notifications.NotificationConstants.EXTRA_REMINDER_MINUTE
import com.habitao.system.notifications.NotificationConstants.EXTRA_SCHEDULED_DAYS
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

class HabitReminderScheduler
    constructor(
        private val context: Context,
        private val alarmManager: AlarmManager,
        private val habitRepository: HabitRepository,
    ) : HabitScheduler {
        override fun scheduleReminder(
            habitId: String,
            habitTitle: String,
            time: LocalTime,
            frequencyType: FrequencyType,
            scheduledDays: Set<DayOfWeek>,
        ) {
            val triggerAt = calculateNextTrigger(time, frequencyType, scheduledDays)
            val pendingIntent = buildReminderPendingIntent(habitId, habitTitle, time, frequencyType, scheduledDays)

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

        override fun cancelReminder(habitId: String) {
            val pendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    habitId.hashCode(),
                    Intent(context, HabitReminderReceiver::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            alarmManager.cancel(pendingIntent)
        }

        override suspend fun rescheduleAllReminders() {
            val habits = habitRepository.getAllHabits().getOrElse { emptyList() }
            habits.filter { it.reminderEnabled && it.reminderTime != null }
                .forEach { habit ->
                    scheduleReminder(
                        habitId = habit.id,
                        habitTitle = habit.title,
                        time = habit.reminderTime!!,
                        frequencyType = habit.frequencyType,
                        scheduledDays = habit.scheduledDays,
                    )
                }
        }

        private fun buildReminderPendingIntent(
            habitId: String,
            habitTitle: String,
            time: LocalTime,
            frequencyType: FrequencyType,
            scheduledDays: Set<DayOfWeek>,
        ): PendingIntent {
            val intent =
                Intent(context, HabitReminderReceiver::class.java).apply {
                    action = ACTION_HABIT_REMINDER
                    putExtra(EXTRA_HABIT_ID, habitId)
                    putExtra(EXTRA_HABIT_TITLE, habitTitle)
                    putExtra(EXTRA_REMINDER_HOUR, time.hour)
                    putExtra(EXTRA_REMINDER_MINUTE, time.minute)
                    putExtra(EXTRA_FREQUENCY_TYPE, frequencyType.name)
                    putStringArrayListExtra(
                        EXTRA_SCHEDULED_DAYS,
                        ArrayList(scheduledDays.map { it.name }),
                    )
                }
            return PendingIntent.getBroadcast(
                context,
                habitId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun calculateNextTrigger(
            time: LocalTime,
            frequencyType: FrequencyType,
            scheduledDays: Set<DayOfWeek>,
        ): Long {
            val timeZone = TimeZone.currentSystemDefault()
            val nowInstant = Clock.System.now()
            val now = nowInstant.toLocalDateTime(timeZone)
            var nextDate = now.date
            val todayTrigger = nextDate.atTime(time.hour, time.minute).toInstant(timeZone)
            if (todayTrigger <= nowInstant) {
                nextDate = nextDate.plus(1, DateTimeUnit.DAY)
            }
            if (frequencyType == FrequencyType.SPECIFIC_DAYS && scheduledDays.isNotEmpty()) {
                var attempts = 0
                while (attempts < 7 && nextDate.dayOfWeek.toDomainDay() !in scheduledDays) {
                    nextDate = nextDate.plus(1, DateTimeUnit.DAY)
                    attempts++
                }
            }
            return nextDate.atTime(time.hour, time.minute).toInstant(timeZone).toEpochMilliseconds()
        }
    }
