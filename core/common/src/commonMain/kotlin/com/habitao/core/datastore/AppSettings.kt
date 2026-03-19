package com.habitao.core.datastore

internal const val DEFAULT_MAX_VISIBLE_TABS = 4
internal const val DEFAULT_THEME_MODE = "SYSTEM"
internal const val DEFAULT_STATS_GRAPH_TYPE = "BAR"

data class AppSettings(
    val bottomNavTabs: List<String> = emptyList(),
    val defaultLaunchTab: String = "habits",
    val maxVisibleTabs: Int = DEFAULT_MAX_VISIBLE_TABS,
    val showTabLabels: Boolean = true,
    val themeMode: String = DEFAULT_THEME_MODE,
    val statsGraphType: String = DEFAULT_STATS_GRAPH_TYPE,
    val habitRemindersEnabled: Boolean = true,
    val taskRemindersEnabled: Boolean = true,
    val pomodoroNotificationsEnabled: Boolean = true,
)
