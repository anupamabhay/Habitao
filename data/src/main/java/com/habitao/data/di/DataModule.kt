package com.habitao.data.di

import android.content.Context
import androidx.room.Room
import com.habitao.data.local.dao.HabitDao
import com.habitao.data.local.dao.HabitLogDao
import com.habitao.data.local.database.HabitaoDatabase
import com.habitao.data.repository.HabitRepositoryImpl
import com.habitao.domain.repository.HabitRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

/**
 * Hilt module for data layer dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindHabitRepository(
        impl: HabitRepositoryImpl
    ): HabitRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): HabitaoDatabase {
        return Room.databaseBuilder(
            context,
            HabitaoDatabase::class.java,
            HabitaoDatabase.DATABASE_NAME
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
    fun provideCoroutineDispatcher(): kotlinx.coroutines.CoroutineDispatcher {
        return Dispatchers.IO
    }
}
