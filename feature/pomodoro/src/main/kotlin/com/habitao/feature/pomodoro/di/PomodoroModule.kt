package com.habitao.feature.pomodoro.di

import com.habitao.feature.pomodoro.service.PomodoroPreferences
import com.habitao.feature.pomodoro.service.TimerStateHolder
import com.habitao.feature.pomodoro.viewmodel.PomodoroViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module

val pomodoroModule = module {
    single { TimerStateHolder() }
    single { PomodoroPreferences(androidContext()) }
    viewModel { PomodoroViewModel(get(), get(), get(), get(), get(), androidContext()) }
}
