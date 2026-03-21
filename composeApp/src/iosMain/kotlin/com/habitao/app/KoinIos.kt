package com.habitao.app

import com.habitao.data.di.allIosDataModules
import com.habitao.domain.notification.HabitScheduler
import com.habitao.domain.notification.IosHabitScheduler
import com.habitao.domain.notification.IosRoutineScheduler
import com.habitao.domain.notification.IosTaskScheduler
import com.habitao.domain.notification.RoutineScheduler
import com.habitao.domain.notification.TaskScheduler
import com.habitao.feature.habits.di.habitsModule
import com.habitao.feature.pomodoro.di.iosPomodoroModule
import com.habitao.feature.pomodoro.di.pomodoroModule
import com.habitao.feature.routines.di.routinesModule
import com.habitao.feature.tasks.di.tasksModule
import org.koin.core.context.startKoin
import org.koin.dsl.module

private val iosSchedulerModule =
    module {
        single<HabitScheduler> { IosHabitScheduler() }
        single<TaskScheduler> { IosTaskScheduler() }
        single<RoutineScheduler> { IosRoutineScheduler() }
    }

fun initKoinIos() {
    startKoin {
        modules(
            allIosDataModules +
                iosSchedulerModule +
                pomodoroModule +
                iosPomodoroModule +
                habitsModule +
                tasksModule +
                routinesModule,
        )
    }
}
