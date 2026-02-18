package com.habitao.system.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.habitao.system.notifications.NotificationConstants.EXTRA_HABIT_ID
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HabitCompletionReceiver : BroadcastReceiver() {
    @Inject
    lateinit var habitCompletionService: HabitCompletionService

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
