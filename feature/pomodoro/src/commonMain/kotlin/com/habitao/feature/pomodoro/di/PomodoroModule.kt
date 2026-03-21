package com.habitao.feature.pomodoro.di

import com.habitao.feature.pomodoro.service.TimerStateHolder
import com.habitao.feature.pomodoro.viewmodel.PomodoroViewModel
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Common Pomodoro Koin module.
 * Platform-specific modules (androidPomodoroModule / iosPomodoroModule) must also be
 * loaded to bind [TimerController] and [PomodoroPreferencesSource].
 */
val pomodoroModule =
    module {
        single { TimerStateHolder() }
        viewModel { PomodoroViewModel(get(), get(), get(), get(), get(), get()) }
    }
