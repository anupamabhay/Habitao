package com.habitao.data.local.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
 

fun createHabitaoDatabase(databasePath: String): HabitaoDatabase {
    return Room.databaseBuilder<HabitaoDatabase>(
        name = databasePath,
    )
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Default)
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
}
