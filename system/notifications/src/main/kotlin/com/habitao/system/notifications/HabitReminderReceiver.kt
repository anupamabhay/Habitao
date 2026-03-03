package com.habitao.system.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.habitao.core.datastore.AppSettingsManager
import com.habitao.system.notifications.NotificationConstants.EXTRA_HABIT_ID
import com.habitao.system.notifications.NotificationConstants.EXTRA_HABIT_TITLE
import com.habitao.system.notifications.NotificationConstants.EXTRA_REMINDER_HOUR
import com.habitao.system.notifications.NotificationConstants.EXTRA_REMINDER_MINUTE
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalTime
import javax.inject.Inject

@AndroidEntryPoint
class HabitReminderReceiver : BroadcastReceiver() {
    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var scheduler: HabitReminderScheduler

    @Inject
    lateinit var appSettingsManager: AppSettingsManager

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val habitId = intent.getStringExtra(EXTRA_HABIT_ID) ?: return
        val habitTitle = intent.getStringExtra(EXTRA_HABIT_TITLE) ?: "Habit"
        val hour = intent.getIntExtra(EXTRA_REMINDER_HOUR, 9)
        val minute = intent.getIntExtra(EXTRA_REMINDER_MINUTE, 0)

        // Check if habit reminders are enabled in settings
        val settings = runBlocking { appSettingsManager.settings.first() }
        if (!settings.habitRemindersEnabled) {
            // Still reschedule so it fires again next time (toggle may be re-enabled)
            scheduler.scheduleReminder(habitId = habitId, habitTitle = habitTitle, time = LocalTime.of(hour, minute))
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || notificationHelper.hasNotificationPermission()) {
            notificationHelper.showReminder(habitId, habitTitle)
        }

        scheduler.scheduleReminder(
            habitId = habitId,
            habitTitle = habitTitle,
            time = LocalTime.of(hour, minute),
        )
    }
}
