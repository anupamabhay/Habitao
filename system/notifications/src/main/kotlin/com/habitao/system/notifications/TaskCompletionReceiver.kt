package com.habitao.system.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.habitao.domain.repository.TaskRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TaskCompletionReceiver : BroadcastReceiver() {
    @Inject
    lateinit var taskRepository: TaskRepository

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
