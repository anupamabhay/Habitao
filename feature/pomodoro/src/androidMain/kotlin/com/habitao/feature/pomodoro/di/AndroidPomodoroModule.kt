package com.habitao.feature.pomodoro.di

import com.habitao.feature.pomodoro.preferences.AndroidPomodoroPreferences
import com.habitao.feature.pomodoro.preferences.PomodoroPreferencesSource
import com.habitao.feature.pomodoro.service.PomodoroPreferences
import com.habitao.feature.pomodoro.timer.AndroidTimerController
import com.habitao.feature.pomodoro.timer.TimerController
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/** Android-specific Koin bindings for [TimerController] and [PomodoroPreferencesSource]. */
val androidPomodoroModule =
    module {
        single { PomodoroPreferences(androidContext()) }
        single<PomodoroPreferencesSource> { AndroidPomodoroPreferences(get()) }
        single<TimerController> { AndroidTimerController(androidContext()) }
    }
