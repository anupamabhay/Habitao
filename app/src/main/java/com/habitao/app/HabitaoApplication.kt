package com.habitao.app

import android.app.Application
import android.util.Log
import com.habitao.system.notifications.NotificationHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class HabitaoApplication : Application() {
    @Inject
    lateinit var notificationHelper: NotificationHelper

    override fun onCreate() {
        super.onCreate()
        notificationHelper.createNotificationChannel()
        notificationHelper.createTaskNotificationChannel()
        if (BuildConfig.DEBUG) {
            Log.d("Habitao", "Application started in debug mode")
        }
    }
}
