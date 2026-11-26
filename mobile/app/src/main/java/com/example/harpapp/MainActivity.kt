package com.example.harpapp

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.harpapp.di.AppContainer
import com.example.harpapp.domain.model.ThemeMode
import com.example.harpapp.initialization.AppInitializationState
import com.example.harpapp.presentation.navigation.AppNavGraph
import com.example.harpapp.presentation.viewmodel.SettingsViewModel
import com.example.harpapp.presentation.ui.theme.HARPAppTheme
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val appContainer: AppContainer
        get() = (application as HarpApplication).appContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        val harpApplication = application as HarpApplication
        installSplashScreen().setKeepOnScreenCondition {
            harpApplication.initializationState.value is AppInitializationState.Loading
        }

        super.onCreate(savedInstanceState)

        setContent {
            val initializationState by harpApplication.initializationState.collectAsState()
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = appContainer.settingsViewModelFactory
            )

            val mode by settingsViewModel.themeMode.collectAsState()
            val darkTheme = mode == ThemeMode.DARK

            DisposableEffect(darkTheme) {
                val barStyle = if (darkTheme) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
                onDispose {}
            }

            HARPAppTheme(darkTheme = darkTheme) {
                AppNavGraph(
                    appContainer = appContainer,
                    settingsViewModel = settingsViewModel,
                    initializationState = initializationState,
                    onRetryInitialization = harpApplication::retryInitialization
                )
            }
        }
    }
}
