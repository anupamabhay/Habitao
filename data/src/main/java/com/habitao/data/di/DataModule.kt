package com.habitao.data.di

import android.content.Context
import androidx.room.Room
import com.habitao.data.local.dao.HabitDao
import com.habitao.data.local.dao.HabitLogDao
import com.habitao.data.local.dao.PomodoroSessionDao
import com.habitao.data.local.dao.RoutineDao
import com.habitao.data.local.dao.TaskDao
import com.habitao.data.local.database.HabitaoDatabase
import com.habitao.data.repository.HabitRepositoryImpl
import com.habitao.data.repository.PomodoroRepositoryImpl
import com.habitao.data.repository.RoutineRepositoryImpl
import com.habitao.data.repository.TaskRepositoryImpl
import com.habitao.domain.repository.HabitRepository
import com.habitao.domain.repository.PomodoroRepository
import com.habitao.domain.repository.RoutineRepository
import com.habitao.domain.repository.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

// Hilt module for data layer dependencies
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindHabitRepository(impl: HabitRepositoryImpl): HabitRepository

    @Binds
    @Singleton
    abstract fun bindPomodoroRepository(impl: PomodoroRepositoryImpl): PomodoroRepository

    @Binds
    @Singleton
    abstract fun bindRoutineRepository(impl: RoutineRepositoryImpl): RoutineRepository

    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): HabitaoDatabase {
        return Room.databaseBuilder(
            context,
            HabitaoDatabase::class.java,
            HabitaoDatabase.DATABASE_NAME,
        )
            .fallbackToDestructiveMigration() // TODO: Add proper migrations
            .build()
    }

    @Provides
    fun provideHabitDao(database: HabitaoDatabase): HabitDao {
        return database.habitDao()
    }

    @Provides
    fun provideHabitLogDao(database: HabitaoDatabase): HabitLogDao {
        return database.habitLogDao()
    }

    @Provides
    fun providePomodoroSessionDao(database: HabitaoDatabase): PomodoroSessionDao {
        return database.pomodoroSessionDao()
    }

    @Provides
    fun provideRoutineDao(database: HabitaoDatabase): RoutineDao {
        return database.routineDao()
    }

    @Provides
    fun provideTaskDao(database: HabitaoDatabase): TaskDao {
        return database.taskDao()
    }

    @Provides
    fun provideCoroutineDispatcher(): kotlinx.coroutines.CoroutineDispatcher {
        return Dispatchers.IO
    }
}
