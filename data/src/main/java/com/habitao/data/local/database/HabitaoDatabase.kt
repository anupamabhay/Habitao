package com.habitao.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.habitao.data.local.dao.HabitDao
import com.habitao.data.local.dao.HabitLogDao
import com.habitao.data.local.entity.HabitEntity
import com.habitao.data.local.entity.HabitLogEntity

/**
 * Room Database for Habitao
 * Contains habits, logs, routines, tasks, etc.
 */
@Database(
    entities = [
        HabitEntity::class,
        HabitLogEntity::class,
        // TODO: Add RoutineEntity, RoutineStepEntity, RoutineLogEntity
        // TODO: Add TaskEntity, PomodoroSessionEntity
    ],
    version = 2,
    exportSchema = false,
)
abstract class HabitaoDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao

    abstract fun habitLogDao(): HabitLogDao

    companion object {
        const val DATABASE_NAME = "habitao.db"

        @Volatile
        private var instance: HabitaoDatabase? = null

        fun getInstance(context: Context): HabitaoDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also {
                    instance = it
                }
            }
        }

        private fun buildDatabase(context: Context): HabitaoDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                HabitaoDatabase::class.java,
                DATABASE_NAME,
            )
                .fallbackToDestructiveMigration() // TODO: Add proper migrations for production
                .build()
        }
    }
}
