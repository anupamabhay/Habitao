package com.habitao.feature.tasks.di

import com.habitao.feature.tasks.viewmodel.CreateTaskViewModel
import com.habitao.feature.tasks.viewmodel.GlobalSearchViewModel
import com.habitao.feature.tasks.viewmodel.TasksViewModel
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module

val tasksModule = module {
    viewModel { TasksViewModel(get()) }
    viewModel { CreateTaskViewModel(get(), get()) }
    viewModel { GlobalSearchViewModel(get(), get(), get()) }
}
