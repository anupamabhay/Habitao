package com.habitao.system.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.habitao.domain.repository.TaskRepository
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlin.math.abs

class TaskReminderScheduler
    constructor(
        private val context: Context,
        private val alarmManager: AlarmManager,
        private val taskRepository: TaskRepository,
    ) {
        fun scheduleReminder(
            taskId: String,
            taskTitle: String,
            dueDate: LocalDate,
            dueTime: LocalTime?,
            minutesBefore: Int = 0,
        ) {
            val reminderTime = dueTime ?: LocalTime(hour = 9, minute = 0)
            val triggerDateTime =
                dueDate.atTime(reminderTime.hour, reminderTime.minute)
                    .minus(minutesBefore.toLong(), kotlinx.datetime.DateTimeUnit.MINUTE)
            val triggerMillis = triggerDateTime.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()

            // Don't schedule if in the past
            if (triggerMillis <= System.currentTimeMillis()) return

            val pendingIntent = buildTaskPendingIntent(taskId, taskTitle)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent,
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent,
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent,
                )
            }
        }

        fun cancelReminder(taskId: String) {
            val pendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    abs(taskId.hashCode()) + 20000,
                    Intent(context, TaskReminderReceiver::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            alarmManager.cancel(pendingIntent)
        }

        suspend fun rescheduleAllReminders() {
            val tasks = taskRepository.getAllTasks().getOrElse { emptyList() }
            tasks.filter { !it.isCompleted && it.reminderEnabled && it.dueDate != null }
                .forEach { task ->
                    scheduleReminder(task.id, task.title, task.dueDate!!, task.dueTime)
                }
        }

        private fun buildTaskPendingIntent(
            taskId: String,
            taskTitle: String,
        ): PendingIntent {
            val intent =
                Intent(context, TaskReminderReceiver::class.java).apply {
                    action = NotificationConstants.ACTION_TASK_REMINDER
                    putExtra(NotificationConstants.EXTRA_TASK_ID, taskId)
                    putExtra(NotificationConstants.EXTRA_TASK_TITLE, taskTitle)
                }
            return PendingIntent.getBroadcast(
                context,
                abs(taskId.hashCode()) + 20000,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
