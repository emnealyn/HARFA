package com.example.harpapp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.harpapp.di.AppContainer
import com.example.harpapp.domain.model.ThemeMode
import com.example.harpapp.presentation.navigation.AppNavGraph
import com.example.harpapp.presentation.viewmodel.SettingsViewModel
import com.example.harpapp.presentation.ui.theme.HARPAppTheme
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val appContainer by lazy { AppContainer(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = appContainer.settingsViewModelFactory
            )

            val mode by settingsViewModel.themeMode.collectAsState()

            HARPAppTheme(darkTheme = (mode == ThemeMode.DARK)) {
                AppNavGraph(
                    appContainer = appContainer,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }
}
