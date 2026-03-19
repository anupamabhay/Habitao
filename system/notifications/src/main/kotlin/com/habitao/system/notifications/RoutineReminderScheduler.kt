package com.habitao.system.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.habitao.domain.model.RepeatPattern
import com.habitao.domain.model.Routine
import com.habitao.domain.repository.RoutineRepository
import com.habitao.system.notifications.NotificationConstants.ACTION_ROUTINE_REMINDER
import com.habitao.system.notifications.NotificationConstants.EXTRA_CUSTOM_INTERVAL
import com.habitao.system.notifications.NotificationConstants.EXTRA_REMINDER_HOUR
import com.habitao.system.notifications.NotificationConstants.EXTRA_REMINDER_MINUTE
import com.habitao.system.notifications.NotificationConstants.EXTRA_REPEAT_PATTERN
import com.habitao.system.notifications.NotificationConstants.EXTRA_ROUTINE_ID
import com.habitao.system.notifications.NotificationConstants.EXTRA_ROUTINE_TITLE
import com.habitao.system.notifications.NotificationConstants.EXTRA_SCHEDULED_DAYS
import com.habitao.system.notifications.NotificationConstants.EXTRA_START_DATE
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class RoutineReminderScheduler
    @Inject
    constructor(
        @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
        private val alarmManager: AlarmManager,
        private val routineRepository: RoutineRepository,
    ) {
        fun scheduleReminder(
            routineId: String,
            routineTitle: String,
            time: LocalTime,
            repeatPattern: RepeatPattern = RepeatPattern.DAILY,
            repeatDays: Set<DayOfWeek> = emptySet(),
            customInterval: Int = 1,
            startDate: LocalDate = LocalDate.now(),
        ) {
            val triggerAt = calculateNextTrigger(time, repeatPattern, repeatDays, customInterval, startDate)
            val pendingIntent =
                buildReminderPendingIntent(
                    routineId,
                    routineTitle,
                    time,
                    repeatPattern,
                    repeatDays,
                    customInterval,
                    startDate,
                )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        }

        fun cancelReminder(routineId: String) {
            val pendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    routineId.hashCode() + 10000,
                    Intent(context, RoutineReminderReceiver::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            alarmManager.cancel(pendingIntent)
        }

        suspend fun rescheduleAllReminders() {
            val routinesResult = routineRepository.observeAllRoutines().first()
            val routines: List<Routine> = routinesResult.getOrElse { emptyList() }
            routines.filter { it.reminderEnabled && it.reminderTime != null && !it.isArchived && it.deletedAt == null }
                .forEach { routine ->
                    scheduleReminder(
                        routineId = routine.id,
                        routineTitle = routine.title,
                        time = routine.reminderTime!!,
                        repeatPattern = routine.repeatPattern,
                        repeatDays = routine.repeatDays?.toSet() ?: emptySet(),
                        customInterval = routine.customInterval ?: 1,
                        startDate = routine.startDate,
                    )
                }
        }

        private fun buildReminderPendingIntent(
            routineId: String,
            routineTitle: String,
            time: LocalTime,
            repeatPattern: RepeatPattern,
            repeatDays: Set<DayOfWeek>,
            customInterval: Int,
            startDate: LocalDate,
        ): PendingIntent {
            val intent =
                Intent(context, RoutineReminderReceiver::class.java).apply {
                    action = ACTION_ROUTINE_REMINDER
                    putExtra(EXTRA_ROUTINE_ID, routineId)
                    putExtra(EXTRA_ROUTINE_TITLE, routineTitle)
                    putExtra(EXTRA_REMINDER_HOUR, time.hour)
                    putExtra(EXTRA_REMINDER_MINUTE, time.minute)
                    putExtra(EXTRA_REPEAT_PATTERN, repeatPattern.name)
                    putExtra(EXTRA_CUSTOM_INTERVAL, customInterval)
                    putExtra(EXTRA_START_DATE, startDate.toEpochDay())
                    putStringArrayListExtra(EXTRA_SCHEDULED_DAYS, ArrayList(repeatDays.map { it.name }))
                }
            return PendingIntent.getBroadcast(
                context,
                routineId.hashCode() + 10000,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun calculateNextTrigger(
            time: LocalTime,
            repeatPattern: RepeatPattern,
            repeatDays: Set<DayOfWeek>,
            customInterval: Int,
            startDate: LocalDate,
        ): Long {
            val now = LocalDateTime.now()
            var next = now.withHour(time.hour).withMinute(time.minute).withSecond(0).withNano(0)

            // If the time has already passed today, start checking from tomorrow
            if (!next.isAfter(now)) {
                next = next.plusDays(1)
            }

            // If the routine hasn't started yet, jump ahead to its start date
            if (next.toLocalDate().isBefore(startDate)) {
                next = startDate.atTime(time).withSecond(0).withNano(0)
                // If the start date is today but the time has passed, start tomorrow
                if (!next.isAfter(now)) {
                    next = next.plusDays(1)
                }
            }

            val safeInterval = customInterval.coerceAtLeast(1)

            when (repeatPattern) {
                RepeatPattern.DAILY -> {
                    // next is already correct
                }
                RepeatPattern.WEEKLY, RepeatPattern.SPECIFIC_DATES -> {
                    if (repeatDays.isNotEmpty()) {
                        // Advance day-by-day until we hit a scheduled day (max 7 days)
                        var attempts = 0
                        while (attempts < 7 && next.toLocalDate().dayOfWeek !in repeatDays) {
                            next = next.plusDays(1)
                            attempts++
                        }
                    }
                }
                RepeatPattern.CUSTOM -> {
                    // Calculate mathematically based on days elapsed since start date
                    val nextDate = next.toLocalDate()
                    val daysBetween = ChronoUnit.DAYS.between(startDate, nextDate)

                    if (daysBetween > 0 && daysBetween % safeInterval != 0L) {
                        // How many days into the current cycle we are
                        val daysIntoCycle = daysBetween % safeInterval
                        // How many days to add to reach the next cycle boundary
                        val daysToAdd = safeInterval - daysIntoCycle
                        next = next.plusDays(daysToAdd)
                    }
                }
            }
            return next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
    }
