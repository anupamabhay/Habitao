package com.habitao.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.ConstructedBy
import androidx.room.RoomDatabaseConstructor
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

@ConstructedBy(HabitaoDatabaseConstructor::class)
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
    }
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object HabitaoDatabaseConstructor : RoomDatabaseConstructor<HabitaoDatabase> {
    override fun initialize(): HabitaoDatabase
}
