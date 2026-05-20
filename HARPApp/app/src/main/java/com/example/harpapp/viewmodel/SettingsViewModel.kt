package com.example.harpapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.harpapp.data.ThemeMode
import com.example.harpapp.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repo: SettingsRepository) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = repo.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.LIGHT)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            repo.setThemeMode(mode)
        }
    }
}