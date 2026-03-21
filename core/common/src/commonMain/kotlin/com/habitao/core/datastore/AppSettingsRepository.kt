package com.habitao.core.datastore

import kotlinx.coroutines.flow.Flow

interface AppSettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setBottomNavTabs(tabIds: List<String>)
    suspend fun setDefaultLaunchTab(tabId: String)
    suspend fun setMaxVisibleTabs(count: Int)
    suspend fun setShowTabLabels(show: Boolean)
    suspend fun setThemeMode(themeMode: String)
    suspend fun setStatsGraphType(graphType: String)
    suspend fun setHabitRemindersEnabled(enabled: Boolean)
    suspend fun setTaskRemindersEnabled(enabled: Boolean)
    suspend fun setPomodoroNotificationsEnabled(enabled: Boolean)
}
