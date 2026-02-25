package com.habitao.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private const val APP_SETTINGS_DATASTORE_NAME = "app_settings"
private const val TAB_SEPARATOR = ","
private const val VISIBLE_TAB_COUNT = 4

private val Context.appSettingsDataStore by preferencesDataStore(name = APP_SETTINGS_DATASTORE_NAME)

data class AppSettings(
    val bottomNavTabs: List<String> = emptyList(),
    val defaultLaunchTab: String = "",
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
                    defaultLaunchTab = preferences[DEFAULT_LAUNCH_TAB_KEY].orEmpty(),
                )
            }

    suspend fun setBottomNavTabs(tabIds: List<String>) {
        val serializedTabs =
            tabIds
                .asSequence()
                .map(String::trim)
                .filter(String::isNotEmpty)
                .distinct()
                .take(VISIBLE_TAB_COUNT)
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

    private fun Preferences.bottomNavTabs(): List<String> {
        return this[BOTTOM_NAV_TABS_KEY]
            .orEmpty()
            .split(TAB_SEPARATOR)
            .map(String::trim)
            .filter(String::isNotEmpty)
    }

    companion object {
        private val BOTTOM_NAV_TABS_KEY = stringPreferencesKey("bottom_nav_tabs")
        private val DEFAULT_LAUNCH_TAB_KEY = stringPreferencesKey("default_launch_tab")
    }
}
