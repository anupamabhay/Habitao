package com.habitao.data.local.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

fun createHabitaoDatabase(databasePath: String): HabitaoDatabase {
    return Room.databaseBuilder<HabitaoDatabase>(
        name = databasePath,
    )
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .fallbackToDestructiveMigration()
        .build()
}
