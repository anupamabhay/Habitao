package com.habitao.system.notifications

import android.app.AlarmManager
import android.app.NotificationManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val notificationModule =
    module {
        single { androidContext().getSystemService(AlarmManager::class.java) }
        single { androidContext().getSystemService(NotificationManager::class.java) }
        single { NotificationHelper(androidContext(), get()) }
        single { HabitReminderScheduler(androidContext(), get(), get()) }
        single { TaskReminderScheduler(androidContext(), get(), get()) }
        single { RoutineReminderScheduler(androidContext(), get(), get()) }
        single { HabitCompletionService(get()) }
    }
