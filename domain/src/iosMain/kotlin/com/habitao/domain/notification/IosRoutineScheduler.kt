package com.habitao.domain.notification

import com.habitao.domain.model.DayOfWeek
import com.habitao.domain.model.RepeatPattern
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.Foundation.NSDateComponents

/**
 * iOS implementation of [RoutineScheduler] using UNUserNotificationCenter.
 */
class IosRoutineScheduler : RoutineScheduler {

    private val center = UNUserNotificationCenter.currentNotificationCenter()

    init {
        requestPermission()
    }

    private fun requestPermission() {
        center.requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
        ) { _, _ -> }
    }

    override fun scheduleReminder(
        routineId: String,
        routineTitle: String,
        time: LocalTime,
        repeatPattern: RepeatPattern,
        repeatDays: Set<DayOfWeek>,
        customInterval: Int,
        startDate: LocalDate,
    ) {
        cancelReminder(routineId)

        when (repeatPattern) {
            RepeatPattern.DAILY -> {
                val trigger = createTimeTrigger(time.hour, time.minute)
                val content = createContent("Routine Reminder", "Time for: $routineTitle")
                val request = UNNotificationRequest.requestWithIdentifier(
                    "routine_${routineId}_daily",
                    content,
                    trigger
                )
                center.addNotificationRequest(request, null)
            }

            RepeatPattern.WEEKLY, RepeatPattern.SPECIFIC_DATES -> {
                for (day in repeatDays) {
                    val weekday = day.toIosWeekday()
                    val dateComponents = NSDateComponents().apply {
                        this.hour = time.hour.toLong()
                        this.minute = time.minute.toLong()
                        this.weekday = weekday.toLong()
                    }
                    val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
                        dateComponents, repeats = true
                    )
                    val content = createContent("Routine Reminder", "Time for: $routineTitle")
                    val request = UNNotificationRequest.requestWithIdentifier(
                        "routine_${routineId}_${day.name}",
                        content,
                        trigger
                    )
                    center.addNotificationRequest(request, null)
                }
            }

            RepeatPattern.CUSTOM -> {
                // Custom interval: schedule a daily notification and let app logic filter
                val trigger = createTimeTrigger(time.hour, time.minute)
                val content = createContent("Routine Reminder", "Time for: $routineTitle")
                val request = UNNotificationRequest.requestWithIdentifier(
                    "routine_${routineId}_daily",
                    content,
                    trigger
                )
                center.addNotificationRequest(request, null)
            }
        }
    }

    override fun cancelReminder(routineId: String) {
        val identifiers = listOf("routine_${routineId}_daily") +
            DayOfWeek.entries.map { "routine_${routineId}_${it.name}" }
        center.removePendingNotificationRequestsWithIdentifiers(identifiers)
    }

    override suspend fun rescheduleAllReminders() {
        // No-op: reminders are scheduled when routines are created/updated
    }

    private fun createTimeTrigger(hour: Int, minute: Int): UNCalendarNotificationTrigger {
        val dateComponents = NSDateComponents().apply {
            this.hour = hour.toLong()
            this.minute = minute.toLong()
        }
        return UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
            dateComponents, repeats = true
        )
    }

    private fun createContent(title: String, body: String): UNMutableNotificationContent {
        return UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(body)
            setSound(platform.UserNotifications.UNNotificationSound.defaultSound())
        }
    }
}

/** Map domain DayOfWeek to iOS weekday (Sunday=1, Monday=2, ... Saturday=7). */
private fun DayOfWeek.toIosWeekday(): Int = when (this) {
    DayOfWeek.SUNDAY -> 1
    DayOfWeek.MONDAY -> 2
    DayOfWeek.TUESDAY -> 3
    DayOfWeek.WEDNESDAY -> 4
    DayOfWeek.THURSDAY -> 5
    DayOfWeek.FRIDAY -> 6
    DayOfWeek.SATURDAY -> 7
}
