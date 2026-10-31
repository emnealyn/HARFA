package com.example.harpapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.harpapp.data.ThemeMode
import com.example.harpapp.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(private val repo: SettingsRepository) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = repo.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.LIGHT)
    private val _currentLanguage = MutableStateFlow(AppCompatDelegate.getApplicationLocales().toLanguageTags())
    val currentLanguage = _currentLanguage.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            repo.setThemeMode(mode)
        }
    }
    fun changeAppLanguage(languageCode: String) {
        val appLocale = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
        _currentLanguage.value = languageCode
    }
}
