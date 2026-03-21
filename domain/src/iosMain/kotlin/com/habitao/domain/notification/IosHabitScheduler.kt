package com.habitao.domain.notification

import com.habitao.domain.model.DayOfWeek
import com.habitao.domain.model.FrequencyType
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
 * iOS implementation of [HabitScheduler] using UNUserNotificationCenter.
 */
class IosHabitScheduler : HabitScheduler {

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
        habitId: String,
        habitTitle: String,
        time: LocalTime,
        frequencyType: FrequencyType,
        scheduledDays: Set<DayOfWeek>,
    ) {
        // Cancel existing reminders for this habit
        cancelReminder(habitId)

        when (frequencyType) {
            FrequencyType.DAILY -> {
                // Single daily notification
                val trigger = createTimeTrigger(time.hour, time.minute)
                val content = createContent("Habit Reminder", "Time to: $habitTitle")
                val request = UNNotificationRequest.requestWithIdentifier(
                    "habit_${habitId}_daily",
                    content,
                    trigger
                )
                center.addNotificationRequest(request, null)
            }

            FrequencyType.SPECIFIC_DAYS -> {
                // One notification per scheduled day
                for (day in scheduledDays) {
                    val weekday = day.toIosWeekday()
                    val dateComponents = NSDateComponents().apply {
                        this.hour = time.hour.toLong()
                        this.minute = time.minute.toLong()
                        this.weekday = weekday.toLong()
                    }
                    val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
                        dateComponents, repeats = true
                    )
                    val content = createContent("Habit Reminder", "Time to: $habitTitle")
                    val request = UNNotificationRequest.requestWithIdentifier(
                        "habit_${habitId}_${day.name}",
                        content,
                        trigger
                    )
                    center.addNotificationRequest(request, null)
                }
            }

            FrequencyType.TIMES_PER_WEEK,
            FrequencyType.EVERY_X_DAYS -> {
                // Fallback: schedule daily, let the app logic decide when to show
                val trigger = createTimeTrigger(time.hour, time.minute)
                val content = createContent("Habit Reminder", "Time to: $habitTitle")
                val request = UNNotificationRequest.requestWithIdentifier(
                    "habit_${habitId}_daily",
                    content,
                    trigger
                )
                center.addNotificationRequest(request, null)
            }
        }
    }

    override fun cancelReminder(habitId: String) {
        val identifiers = listOf("habit_${habitId}_daily") +
            DayOfWeek.entries.map { "habit_${habitId}_${it.name}" }
        center.removePendingNotificationRequestsWithIdentifiers(identifiers)
    }

    override suspend fun rescheduleAllReminders() {
        // Would need access to the repository to iterate all habits
        // No-op for now; reminders are scheduled when habits are created/updated
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
