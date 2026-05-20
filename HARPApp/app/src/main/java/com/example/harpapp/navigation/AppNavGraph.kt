package com.example.harpapp.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
// remember is not needed here
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.harpapp.ui.components.NavBar
import com.example.harpapp.ui.screens.home.HomeScreen
import com.example.harpapp.ui.screens.settings.SettingsScreen
import com.example.harpapp.ui.screens.song.SongScreen
import com.example.harpapp.ui.screens.splash.SplashScreen
import com.example.harpapp.ui.screens.bluetooth.BluetoothScreen
import com.example.harpapp.viewmodel.HomeViewModel
import com.example.harpapp.viewmodel.SettingsViewModel

@Composable
fun AppNavGraph(
    settingsViewModel: SettingsViewModel
) {

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val homeViewModel: HomeViewModel = viewModel()

    // settingsViewModel is provided by caller (MainActivity)

    Scaffold(
        bottomBar = {
            if (currentRoute in listOf(Routes.HOME, Routes.SETTINGS, Routes.SONG)) {
                NavBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = androidx.compose.ui.Modifier.padding(innerPadding)
        ) {
            composable(Routes.SPLASH) {
                SplashScreen()
            }
            composable(Routes.HOME) {
                HomeScreen(viewModel = homeViewModel)
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(viewModel = settingsViewModel)
            }
            composable(Routes.SONG) {
                SongScreen()
            }
            composable(Routes.BLUETOOTH) {
                BluetoothScreen()
            }
        }
    }
}
