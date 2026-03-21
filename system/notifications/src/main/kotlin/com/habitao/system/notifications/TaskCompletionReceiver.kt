package com.habitao.system.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.habitao.domain.repository.TaskRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskCompletionReceiver : BroadcastReceiver(), KoinComponent {
    private val taskRepository: TaskRepository by inject()

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val taskId = intent.getStringExtra(NotificationConstants.EXTRA_TASK_ID) ?: return
        val pendingResult = goAsync()

        NotificationManagerCompat.from(context).cancel(taskId.hashCode() + 10000)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val task = taskRepository.getTaskById(taskId).getOrNull() ?: return@launch
                taskRepository.updateTask(
                    task.copy(
                        isCompleted = true,
                        completedAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
