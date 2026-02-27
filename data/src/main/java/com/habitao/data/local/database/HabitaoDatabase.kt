package com.habitao.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.habitao.data.local.dao.HabitDao
import com.habitao.data.local.dao.HabitLogDao
import com.habitao.data.local.dao.PomodoroSessionDao
import com.habitao.data.local.dao.RoutineDao
import com.habitao.data.local.dao.TaskDao
import com.habitao.data.local.entity.HabitEntity
import com.habitao.data.local.entity.HabitLogEntity
import com.habitao.data.local.entity.PomodoroSessionEntity
import com.habitao.data.local.entity.RoutineEntity
import com.habitao.data.local.entity.RoutineLogEntity
import com.habitao.data.local.entity.RoutineStepEntity
import com.habitao.data.local.entity.TaskEntity

// Room Database for Habitao
@Database(
    entities = [
        HabitEntity::class,
        HabitLogEntity::class,
        PomodoroSessionEntity::class,
        RoutineEntity::class,
        RoutineStepEntity::class,
        RoutineLogEntity::class,
        TaskEntity::class,
    ],
    version = 5,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class HabitaoDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao

    abstract fun habitLogDao(): HabitLogDao

    abstract fun pomodoroSessionDao(): PomodoroSessionDao

    abstract fun routineDao(): RoutineDao

    abstract fun taskDao(): TaskDao

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
                .addMigrations(MIGRATION_4_5)
                .fallbackToDestructiveMigration() // TODO: Add proper migrations for production
                .build()
        }

        val MIGRATION_4_5 =
            object : Migration(4, 5) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE pomodoro_sessions ADD COLUMN linkedTaskId TEXT")
                    db.execSQL("ALTER TABLE pomodoro_sessions ADD COLUMN linkedHabitId TEXT")
                }
            }
    }
}
