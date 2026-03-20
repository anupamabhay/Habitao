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
import com.habitao.domain.model.DayOfWeek
import com.habitao.domain.model.toDomainDay
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.toInstant

class RoutineReminderScheduler
    constructor(
        private val context: Context,
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
            startDate: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
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
                    putExtra(EXTRA_START_DATE, startDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds())
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
            val timeZone = TimeZone.currentSystemDefault()
            val nowInstant = Clock.System.now()
            val nowDateTime = nowInstant.toLocalDateTime(timeZone)
            var nextDate = nowDateTime.date
            val todayTrigger = nextDate.atTime(time.hour, time.minute).toInstant(timeZone)

            if (todayTrigger <= nowInstant) {
                nextDate = nextDate.plus(1, DateTimeUnit.DAY)
            }

            if (nextDate < startDate) {
                nextDate = startDate
                val startTrigger = nextDate.atTime(time.hour, time.minute).toInstant(timeZone)
                if (startTrigger <= nowInstant) {
                    nextDate = nextDate.plus(1, DateTimeUnit.DAY)
                }
            }

            val safeInterval = customInterval.coerceAtLeast(1)

            when (repeatPattern) {
                RepeatPattern.DAILY -> {
                    // next is already correct
                }
                RepeatPattern.WEEKLY, RepeatPattern.SPECIFIC_DATES -> {
                    if (repeatDays.isNotEmpty()) {
                        var attempts = 0
                        while (attempts < 7 && nextDate.dayOfWeek.toDomainDay() !in repeatDays) {
                            nextDate = nextDate.plus(1, DateTimeUnit.DAY)
                            attempts++
                        }
                    }
                }
                RepeatPattern.CUSTOM -> {
                    val daysBetween = startDate.daysUntil(nextDate).toLong()

                    if (daysBetween > 0 && daysBetween % safeInterval != 0L) {
                        val daysIntoCycle = daysBetween % safeInterval
                        val daysToAdd = safeInterval - daysIntoCycle
                        nextDate = nextDate.plus(daysToAdd, DateTimeUnit.DAY)
                    }
                }
            }
            return nextDate.atTime(time.hour, time.minute).toInstant(timeZone).toEpochMilliseconds()
        }
    }
