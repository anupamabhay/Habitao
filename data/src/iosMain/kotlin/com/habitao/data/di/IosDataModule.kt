package com.habitao.data.di

import com.habitao.core.datastore.AppSettingsManager
import com.habitao.core.datastore.AppSettingsRepository
import com.habitao.data.local.database.HabitaoDatabase
import com.habitao.data.local.database.createHabitaoDatabase
import org.koin.dsl.module
import platform.Foundation.NSHomeDirectory

val iosDataModule =
    module {
        single<HabitaoDatabase> {
            val dbPath = NSHomeDirectory() + "/Documents/habitao.db"
            createHabitaoDatabase(dbPath)
        }
        single<AppSettingsRepository> { AppSettingsManager() }
    }

val allIosDataModules = dataModules + iosDataModule
