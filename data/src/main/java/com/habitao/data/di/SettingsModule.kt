package com.habitao.data.di

import android.content.Context
import com.habitao.core.datastore.AppSettingsManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SettingsModule {

    @Provides
    @Singleton
    fun provideAppSettingsManager(
        @ApplicationContext context: Context
    ): AppSettingsManager {
        return AppSettingsManager(context)
    }
}