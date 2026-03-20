package com.habitao.system.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.habitao.core.datastore.AppSettingsRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TaskReminderReceiver : BroadcastReceiver(), KoinComponent {
    private val notificationHelper: NotificationHelper by inject()
    private val appSettingsManager: AppSettingsRepository by inject()

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
