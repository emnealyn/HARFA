package com.example.harpapp.domain.repository

import com.example.harpapp.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val themeMode: Flow<ThemeMode>
    val language: Flow<String>

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setLanguage(languageCode: String)
}
