package com.example.harpapp.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.harpapp.domain.model.ThemeMode
import com.example.harpapp.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val THEME_MODE = intPreferencesKey("theme_mode")
private val APP_LANGUAGE = stringPreferencesKey("app_language")

class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {
    override val themeMode: Flow<ThemeMode> = dataStore.data
        .map { preferences -> ThemeMode.fromInt(preferences[THEME_MODE]) }

    override val language: Flow<String> = dataStore.data
        .map { preferences -> preferences[APP_LANGUAGE] ?: "en" }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode.value
        }
    }

    override suspend fun setLanguage(languageCode: String) {
        dataStore.edit { preferences ->
            preferences[APP_LANGUAGE] = languageCode
        }
    }
}
