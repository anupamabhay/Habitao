package com.habitao.system.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.habitao.core.datastore.AppSettingsManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TaskReminderReceiver : BroadcastReceiver() {
    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var appSettingsManager: AppSettingsManager

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val taskId = intent.getStringExtra(NotificationConstants.EXTRA_TASK_ID) ?: return
        val taskTitle = intent.getStringExtra(NotificationConstants.EXTRA_TASK_TITLE) ?: "Task"

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Check if task reminders are enabled in settings
                val settings = appSettingsManager.settings.first()
                if (!settings.taskRemindersEnabled) return@launch

                val hasPermission =
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                        notificationHelper.hasNotificationPermission()
                if (hasPermission) {
                    notificationHelper.showTaskReminder(taskId, taskTitle)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
