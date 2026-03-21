package com.habitao.system.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.habitao.system.notifications.NotificationConstants.EXTRA_HABIT_ID
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HabitCompletionReceiver : BroadcastReceiver(), KoinComponent {
    private val habitCompletionService: HabitCompletionService by inject()

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val habitId = intent.getStringExtra(EXTRA_HABIT_ID) ?: return
        CoroutineScope(Dispatchers.IO).launch {
            habitCompletionService.markHabitComplete(habitId)
        }
    }
}
