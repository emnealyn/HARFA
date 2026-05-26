package com.example.harpapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.harpapp.data.dataStore
import com.example.harpapp.navigation.AppNavGraph
import com.example.harpapp.repository.SettingsRepository
import com.example.harpapp.viewmodel.SettingsViewModel
import com.example.harpapp.viewmodel.SettingsViewModelFactory
import com.example.harpapp.ui.theme.HARPAppTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current
            val repo = remember { SettingsRepository(context.dataStore) }
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModelFactory(repo)
            )

            val mode by settingsViewModel.themeMode.collectAsState()

            HARPAppTheme(darkTheme = (mode == com.example.harpapp.data.ThemeMode.DARK)) {
                AppNavGraph(settingsViewModel = settingsViewModel)
            }
        }
    }
}
