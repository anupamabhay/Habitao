package com.habitao.app

import android.app.Application
import android.util.Log
import com.habitao.data.backup.BackupManager
import com.habitao.data.di.allAndroidDataModules
import com.habitao.feature.habits.di.habitsModule
import com.habitao.feature.pomodoro.di.pomodoroModule
import com.habitao.feature.routines.di.routinesModule
import com.habitao.feature.tasks.di.tasksModule
import com.habitao.system.notifications.NotificationHelper
import com.habitao.system.notifications.notificationModule
import org.koin.core.module.Module
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class HabitaoApplication : Application() {
    private val appModule: Module =
        module {
            single { BackupManager(androidContext(), get(), get(), get(), get(), get(), get()) }
        }

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@HabitaoApplication)
            modules(
                allAndroidDataModules +
                    notificationModule +
                    habitsModule +
                    tasksModule +
                    routinesModule +
                    pomodoroModule +
                    appModule,
            )
        }

        val notificationHelper = GlobalContext.get().get<NotificationHelper>()
        notificationHelper.createNotificationChannel()
        notificationHelper.createTaskNotificationChannel()
        if (BuildConfig.DEBUG) {
            Log.d("Habitao", "Application started in debug mode")
        }
    }
}
