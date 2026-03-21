package com.habitao.data.di

import com.habitao.core.datastore.AppSettingsManager
import com.habitao.core.datastore.AppSettingsRepository
import com.habitao.data.local.database.HabitaoDatabase
import com.habitao.data.local.database.createHabitaoDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidDataModule = module {
    single<HabitaoDatabase> { createHabitaoDatabase(androidContext()) }
    single<AppSettingsRepository> { AppSettingsManager(androidContext()) }
}

val allAndroidDataModules = dataModules + androidDataModule
