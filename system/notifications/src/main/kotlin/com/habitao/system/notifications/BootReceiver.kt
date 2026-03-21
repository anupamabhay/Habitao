package com.habitao.system.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver(), KoinComponent {
    private val habitScheduler: HabitReminderScheduler by inject()
    private val taskScheduler: TaskReminderScheduler by inject()
    private val routineScheduler: RoutineReminderScheduler by inject()

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    habitScheduler.rescheduleAllReminders()
                    taskScheduler.rescheduleAllReminders()
                    routineScheduler.rescheduleAllReminders()
                } catch (e: Exception) {
                    android.util.Log.e("BootReceiver", "Failed to reschedule reminders", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
