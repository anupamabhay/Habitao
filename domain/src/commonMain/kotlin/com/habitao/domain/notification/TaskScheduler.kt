package com.habitao.domain.notification

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

interface TaskScheduler {
    fun scheduleReminder(
        taskId: String,
        taskTitle: String,
        dueDate: LocalDate,
        dueTime: LocalTime?,
        minutesBefore: Int = 0,
    )

    fun cancelReminder(taskId: String)

    suspend fun rescheduleAllReminders()
}

/** No-op implementation for platforms without native scheduling (e.g. iOS). */
class NoOpTaskScheduler : TaskScheduler {
    override fun scheduleReminder(
        taskId: String,
        taskTitle: String,
        dueDate: LocalDate,
        dueTime: LocalTime?,
        minutesBefore: Int,
    ) = Unit

    override fun cancelReminder(taskId: String) = Unit

    override suspend fun rescheduleAllReminders() = Unit
}
