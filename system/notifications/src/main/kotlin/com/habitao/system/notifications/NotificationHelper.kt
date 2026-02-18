package com.habitao.system.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.habitao.system.notifications.NotificationConstants.ACTION_MARK_COMPLETE
import com.habitao.system.notifications.NotificationConstants.CHANNEL_ID
import com.habitao.system.notifications.NotificationConstants.CHANNEL_NAME
import com.habitao.system.notifications.NotificationConstants.EXTRA_HABIT_ID
import com.habitao.system.notifications.NotificationConstants.EXTRA_HABIT_TITLE

class NotificationHelper(
    private val context: Context,
    private val notificationManager: NotificationManager,
) {
    fun createNotificationChannel() {
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Habit reminder notifications"
            }
        notificationManager.createNotificationChannel(channel)
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showReminder(
        habitId: String,
        habitTitle: String,
    ) {
        val contentIntent =
            context.packageManager.getLaunchIntentForPackage(context.packageName)?.let { intent ->
                PendingIntent.getActivity(
                    context,
                    habitId.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            }

        val completeIntent =
            Intent(context, HabitCompletionReceiver::class.java).apply {
                action = ACTION_MARK_COMPLETE
                putExtra(EXTRA_HABIT_ID, habitId)
                putExtra(EXTRA_HABIT_TITLE, habitTitle)
            }

        val completePendingIntent =
            PendingIntent.getBroadcast(
                context,
                habitId.hashCode() + 1,
                completeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val notification =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Habit Reminder")
                .setContentText("Time to: $habitTitle")
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .addAction(android.R.drawable.ic_menu_agenda, "Mark Complete", completePendingIntent)
                .build()

        NotificationManagerCompat.from(context).notify(habitId.hashCode(), notification)
    }
}
