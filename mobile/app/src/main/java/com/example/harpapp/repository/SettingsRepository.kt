package com.example.harpapp.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.Preferences
import com.example.harpapp.data.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val THEME_MODE = intPreferencesKey("theme_mode")
private val APP_LANGUAGE = stringPreferencesKey("app_language")

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val themeModeFlow: Flow<ThemeMode> = dataStore.data
        .map { prefs ->
            val raw: Int? = prefs[THEME_MODE]
            when (raw) {
                0 -> ThemeMode.LIGHT
                1 -> ThemeMode.DARK
                else -> ThemeMode.LIGHT
            }
        }

    val languageFlow: Flow<String> = dataStore.data
        .map { prefs ->
            prefs[APP_LANGUAGE] ?: "en" // Domyślnie angielski
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[THEME_MODE] = mode.value
        }
    }

    suspend fun setLanguage(languageCode: String) {
        dataStore.edit { prefs ->
            prefs[APP_LANGUAGE] = languageCode
        }
    }
}
