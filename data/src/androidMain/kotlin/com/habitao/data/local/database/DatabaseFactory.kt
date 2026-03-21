package com.habitao.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

fun createHabitaoDatabase(context: Context): HabitaoDatabase {
    return Room.databaseBuilder(
        context.applicationContext,
        HabitaoDatabase::class.java,
        HabitaoDatabase.DATABASE_NAME,
    )
        .addMigrations(MIGRATION_4_5)
        .fallbackToDestructiveMigration()
        .build()
}

private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE pomodoro_sessions ADD COLUMN linkedTaskId TEXT")
        db.execSQL("ALTER TABLE pomodoro_sessions ADD COLUMN linkedHabitId TEXT")
    }
}
