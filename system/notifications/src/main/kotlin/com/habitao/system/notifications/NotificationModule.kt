package com.habitao.system.notifications

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NotificationModule {
    @Provides
    fun provideAlarmManager(
        @ApplicationContext context: Context,
    ): AlarmManager = context.getSystemService(AlarmManager::class.java)

    @Provides
    fun provideNotificationManager(
        @ApplicationContext context: Context,
    ): NotificationManager = context.getSystemService(NotificationManager::class.java)

    @Provides
    @Singleton
    fun provideNotificationHelper(
        @ApplicationContext context: Context,
        notificationManager: NotificationManager,
    ): NotificationHelper = NotificationHelper(context, notificationManager)
}
