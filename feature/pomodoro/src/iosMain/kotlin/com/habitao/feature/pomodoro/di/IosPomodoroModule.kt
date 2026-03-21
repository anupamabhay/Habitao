package com.habitao.feature.pomodoro.di

import com.habitao.feature.pomodoro.preferences.IosPomodoroPreferences
import com.habitao.feature.pomodoro.preferences.PomodoroPreferencesSource
import com.habitao.feature.pomodoro.timer.IosTimerController
import com.habitao.feature.pomodoro.timer.TimerController
import org.koin.dsl.module

/** iOS-specific Koin bindings for [TimerController] and [PomodoroPreferencesSource]. */
val iosPomodoroModule =
    module {
        single<PomodoroPreferencesSource> { IosPomodoroPreferences() }
        single<TimerController> { IosTimerController(get(), get()) }
    }
