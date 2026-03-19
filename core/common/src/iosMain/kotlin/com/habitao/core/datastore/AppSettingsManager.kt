package com.habitao.core.datastore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * iOS stub for AppSettingsManager.
 * TODO: Implement using NSUserDefaults or iOS DataStore when needed.
 */
class AppSettingsManager : AppSettingsRepository {
    private val _settings = MutableStateFlow(AppSettings())

    override val settings: Flow<AppSettings> = _settings

    override suspend fun setBottomNavTabs(tabIds: List<String>) {
        _settings.value = _settings.value.copy(bottomNavTabs = tabIds)
    }

    override suspend fun setDefaultLaunchTab(tabId: String) {
        _settings.value = _settings.value.copy(defaultLaunchTab = tabId)
    }

    override suspend fun setMaxVisibleTabs(count: Int) {
        _settings.value = _settings.value.copy(maxVisibleTabs = count.coerceIn(3, 5))
    }

    override suspend fun setShowTabLabels(show: Boolean) {
        _settings.value = _settings.value.copy(showTabLabels = show)
    }

    override suspend fun setThemeMode(themeMode: String) {
        _settings.value = _settings.value.copy(themeMode = themeMode)
    }

    override suspend fun setStatsGraphType(graphType: String) {
        _settings.value = _settings.value.copy(statsGraphType = graphType)
    }

    override suspend fun setHabitRemindersEnabled(enabled: Boolean) {
        _settings.value = _settings.value.copy(habitRemindersEnabled = enabled)
    }

    override suspend fun setTaskRemindersEnabled(enabled: Boolean) {
        _settings.value = _settings.value.copy(taskRemindersEnabled = enabled)
    }

    override suspend fun setPomodoroNotificationsEnabled(enabled: Boolean) {
        _settings.value = _settings.value.copy(pomodoroNotificationsEnabled = enabled)
    }
}
