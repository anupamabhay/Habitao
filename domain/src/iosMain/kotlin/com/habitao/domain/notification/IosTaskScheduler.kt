package com.habitao.domain.notification

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
 * iOS implementation of [TaskScheduler] using UNUserNotificationCenter.
 */
class IosTaskScheduler : TaskScheduler {

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
        taskId: String,
        taskTitle: String,
        dueDate: LocalDate,
        dueTime: LocalTime?,
        minutesBefore: Int,
    ) {
        cancelReminder(taskId)

        val hour = dueTime?.hour ?: 9
        val minute = dueTime?.minute ?: 0

        // Adjust for minutesBefore
        var adjustedHour = hour
        var adjustedMinute = minute - minutesBefore
        while (adjustedMinute < 0) {
            adjustedMinute += 60
            adjustedHour -= 1
        }
        if (adjustedHour < 0) adjustedHour = 0

        val dateComponents = NSDateComponents().apply {
            this.year = dueDate.year.toLong()
            this.month = dueDate.monthNumber.toLong()
            this.day = dueDate.dayOfMonth.toLong()
            this.hour = adjustedHour.toLong()
            this.minute = adjustedMinute.toLong()
        }

        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
            dateComponents, repeats = false
        )

        val content = UNMutableNotificationContent().apply {
            setTitle("Task Reminder")
            setBody("Time to: $taskTitle")
            setSound(platform.UserNotifications.UNNotificationSound.defaultSound())
        }

        val request = UNNotificationRequest.requestWithIdentifier(
            "task_$taskId",
            content,
            trigger
        )
        center.addNotificationRequest(request, null)
    }

    override fun cancelReminder(taskId: String) {
        center.removePendingNotificationRequestsWithIdentifiers(listOf("task_$taskId"))
    }

    override suspend fun rescheduleAllReminders() {
        // No-op: reminders are scheduled when tasks are created/updated
    }
}
