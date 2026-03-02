package com.habitao.system.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TaskReminderReceiver : BroadcastReceiver() {
    @Inject
    lateinit var notificationHelper: NotificationHelper

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val taskId = intent.getStringExtra(NotificationConstants.EXTRA_TASK_ID) ?: return
        val taskTitle = intent.getStringExtra(NotificationConstants.EXTRA_TASK_TITLE) ?: "Task"

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || notificationHelper.hasNotificationPermission()) {
            notificationHelper.showTaskReminder(taskId, taskTitle)
        }
    }
}
