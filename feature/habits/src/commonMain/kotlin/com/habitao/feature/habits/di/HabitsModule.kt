package com.habitao.feature.habits.di

import com.habitao.feature.habits.viewmodel.CreateHabitViewModel
import com.habitao.feature.habits.viewmodel.HabitsViewModel
import com.habitao.feature.habits.viewmodel.StatsViewModel
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module

val habitsModule = module {
    viewModel { HabitsViewModel(get()) }
    viewModel { CreateHabitViewModel(get(), get()) }
    viewModel { StatsViewModel(get(), get(), get(), get(), get(), get(), get()) }
}
