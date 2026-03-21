package com.habitao.feature.routines.di

import com.habitao.feature.routines.viewmodel.CreateRoutineViewModel
import com.habitao.feature.routines.viewmodel.RoutineStatsViewModel
import com.habitao.feature.routines.viewmodel.RoutinesViewModel
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module

val routinesModule = module {
    viewModel { RoutinesViewModel(get()) }
    viewModel { CreateRoutineViewModel(get(), get()) }
    viewModel { RoutineStatsViewModel(get()) }
}
