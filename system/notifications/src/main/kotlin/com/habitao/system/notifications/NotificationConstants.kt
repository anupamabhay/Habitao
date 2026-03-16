package com.habitao.system.notifications

object NotificationConstants {
    // Habit reminders
    const val CHANNEL_ID = "habit_reminders"
    const val CHANNEL_NAME = "Habit Reminders"
    const val EXTRA_HABIT_ID = "HABIT_ID"
    const val EXTRA_HABIT_TITLE = "HABIT_TITLE"
    const val EXTRA_REMINDER_HOUR = "REMINDER_HOUR"
    const val EXTRA_REMINDER_MINUTE = "REMINDER_MINUTE"
    const val EXTRA_FREQUENCY_TYPE = "FREQUENCY_TYPE"
    const val EXTRA_SCHEDULED_DAYS = "SCHEDULED_DAYS"
    const val ACTION_HABIT_REMINDER = "com.habitao.system.notifications.ACTION_HABIT_REMINDER"
    const val ACTION_MARK_COMPLETE = "com.habitao.system.notifications.ACTION_MARK_COMPLETE"

    // Task reminders
    const val TASK_CHANNEL_ID = "task_reminders"
    const val TASK_CHANNEL_NAME = "Task Reminders"
    const val EXTRA_TASK_ID = "TASK_ID"
    const val EXTRA_TASK_TITLE = "TASK_TITLE"
    const val ACTION_TASK_REMINDER = "com.habitao.system.notifications.ACTION_TASK_REMINDER"
    const val ACTION_TASK_MARK_COMPLETE = "com.habitao.system.notifications.ACTION_TASK_MARK_COMPLETE"

    // Routine reminders
    const val ROUTINE_CHANNEL_ID = "routine_reminders"
    const val ROUTINE_CHANNEL_NAME = "Routine Reminders"
    const val EXTRA_ROUTINE_ID = "ROUTINE_ID"
    const val EXTRA_ROUTINE_TITLE = "ROUTINE_TITLE"
    const val ACTION_ROUTINE_REMINDER = "com.habitao.system.notifications.ACTION_ROUTINE_REMINDER"
    const val ACTION_ROUTINE_MARK_COMPLETE = "com.habitao.system.notifications.ACTION_ROUTINE_MARK_COMPLETE"
    const val EXTRA_REPEAT_PATTERN = "REPEAT_PATTERN"
    const val EXTRA_CUSTOM_INTERVAL = "CUSTOM_INTERVAL"
    const val EXTRA_START_DATE = "START_DATE"
}
