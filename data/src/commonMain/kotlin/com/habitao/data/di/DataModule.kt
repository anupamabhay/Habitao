package com.habitao.data.di

import com.habitao.data.local.database.HabitaoDatabase
import com.habitao.data.repository.HabitRepositoryImpl
import com.habitao.data.repository.PomodoroRepositoryImpl
import com.habitao.data.repository.RoutineRepositoryImpl
import com.habitao.data.repository.TaskRepositoryImpl
import com.habitao.domain.repository.HabitRepository
import com.habitao.domain.repository.PomodoroRepository
import com.habitao.domain.repository.RoutineRepository
import com.habitao.domain.repository.TaskRepository
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module

val repositoryModule = module {
    single<HabitRepository> {
        HabitRepositoryImpl(get(), get(), Dispatchers.Default)
    }
    single<PomodoroRepository> {
        PomodoroRepositoryImpl(get(), Dispatchers.Default)
    }
    single<RoutineRepository> {
        RoutineRepositoryImpl(get(), Dispatchers.Default)
    }
    single<TaskRepository> {
        TaskRepositoryImpl(get(), Dispatchers.Default)
    }
}

val daoModule = module {
    single { get<HabitaoDatabase>().habitDao() }
    single { get<HabitaoDatabase>().habitLogDao() }
    single { get<HabitaoDatabase>().pomodoroSessionDao() }
    single { get<HabitaoDatabase>().routineDao() }
    single { get<HabitaoDatabase>().taskDao() }
}

val dataModules = listOf(repositoryModule, daoModule)
