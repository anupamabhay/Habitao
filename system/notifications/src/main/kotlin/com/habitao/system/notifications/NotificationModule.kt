package com.habitao.system.notifications

import android.app.AlarmManager
import android.app.NotificationManager
import com.habitao.domain.notification.HabitScheduler
import com.habitao.domain.notification.RoutineScheduler
import com.habitao.domain.notification.TaskScheduler
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val notificationModule =
    module {
        single { androidContext().getSystemService(AlarmManager::class.java) }
        single { androidContext().getSystemService(NotificationManager::class.java) }
        single { NotificationHelper(androidContext(), get()) }
        single<HabitScheduler> { HabitReminderScheduler(androidContext(), get(), get()) }
        single<TaskScheduler> { TaskReminderScheduler(androidContext(), get(), get()) }
        single<RoutineScheduler> { RoutineReminderScheduler(androidContext(), get(), get()) }
        single { HabitCompletionService(get()) }
    }
