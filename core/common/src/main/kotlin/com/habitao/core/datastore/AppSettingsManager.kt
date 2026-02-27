package com.habitao.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private const val APP_SETTINGS_DATASTORE_NAME = "app_settings"
private const val TAB_SEPARATOR = ","
private const val DEFAULT_MAX_VISIBLE_TABS = 4
private const val DEFAULT_THEME_MODE = "SYSTEM"
private const val DEFAULT_STATS_GRAPH_TYPE = "BAR"

private val Context.appSettingsDataStore by preferencesDataStore(name = APP_SETTINGS_DATASTORE_NAME)

data class AppSettings(
    val bottomNavTabs: List<String> = emptyList(),
    val defaultLaunchTab: String = "habits",
    val maxVisibleTabs: Int = DEFAULT_MAX_VISIBLE_TABS,
    val themeMode: String = DEFAULT_THEME_MODE,
    val statsGraphType: String = DEFAULT_STATS_GRAPH_TYPE,
)

class AppSettingsManager(
    context: Context,
) {
    private val dataStore = context.appSettingsDataStore

    val settings: Flow<AppSettings> =
        dataStore.data
            .catch { error ->
                if (error is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw error
                }
            }
            .map { preferences ->
                AppSettings(
                    bottomNavTabs = preferences.bottomNavTabs(),
                    defaultLaunchTab = preferences[DEFAULT_LAUNCH_TAB_KEY] ?: "habits",
                    maxVisibleTabs = preferences[MAX_VISIBLE_TABS_KEY] ?: DEFAULT_MAX_VISIBLE_TABS,
                    themeMode = preferences.themeMode(),
                    statsGraphType = preferences.statsGraphType(),
                )
            }

    suspend fun setBottomNavTabs(tabIds: List<String>) {
        val serializedTabs =
            tabIds
                .asSequence()
                .map(String::trim)
                .filter(String::isNotEmpty)
                .distinct()
                .joinToString(TAB_SEPARATOR)

        dataStore.edit { preferences ->
            if (serializedTabs.isBlank()) {
                preferences.remove(BOTTOM_NAV_TABS_KEY)
            } else {
                preferences[BOTTOM_NAV_TABS_KEY] = serializedTabs
            }
        }
    }

    suspend fun setDefaultLaunchTab(tabId: String) {
        val normalizedTabId = tabId.trim()
        dataStore.edit { preferences ->
            if (normalizedTabId.isBlank()) {
                preferences.remove(DEFAULT_LAUNCH_TAB_KEY)
            } else {
                preferences[DEFAULT_LAUNCH_TAB_KEY] = normalizedTabId
            }
        }
    }

    suspend fun setMaxVisibleTabs(count: Int) {
        val clamped = count.coerceIn(3, 5)
        dataStore.edit { preferences ->
            preferences[MAX_VISIBLE_TABS_KEY] = clamped
        }
    }

    suspend fun setThemeMode(themeMode: String) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = normalizeThemeMode(themeMode)
        }
    }

    suspend fun setStatsGraphType(graphType: String) {
        dataStore.edit { preferences ->
            preferences[STATS_GRAPH_TYPE_KEY] = normalizeGraphType(graphType)
        }
    }

    private fun Preferences.bottomNavTabs(): List<String> {
        return this[BOTTOM_NAV_TABS_KEY]
            .orEmpty()
            .split(TAB_SEPARATOR)
            .map(String::trim)
            .filter(String::isNotEmpty)
    }

    private fun Preferences.themeMode(): String {
        return normalizeThemeMode(this[THEME_MODE_KEY])
    }

    private fun normalizeThemeMode(themeMode: String?): String {
        return when (themeMode?.trim()?.uppercase()) {
            "LIGHT" -> "LIGHT"
            "DARK" -> "DARK"
            else -> DEFAULT_THEME_MODE
        }
    }

    private fun Preferences.statsGraphType(): String {
        return normalizeGraphType(this[STATS_GRAPH_TYPE_KEY])
    }

    private fun normalizeGraphType(graphType: String?): String {
        return when (graphType?.trim()?.uppercase()) {
            "LINE" -> "LINE"
            else -> DEFAULT_STATS_GRAPH_TYPE
        }
    }

    companion object {
        private val BOTTOM_NAV_TABS_KEY = stringPreferencesKey("bottom_nav_tabs")
        private val DEFAULT_LAUNCH_TAB_KEY = stringPreferencesKey("default_launch_tab")
        private val MAX_VISIBLE_TABS_KEY = intPreferencesKey("max_visible_tabs")
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        private val STATS_GRAPH_TYPE_KEY = stringPreferencesKey("stats_graph_type")
    }
}
