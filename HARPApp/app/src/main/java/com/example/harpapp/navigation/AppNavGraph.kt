package com.example.harpapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.harpapp.ui.screens.home.HomeScreen
import com.example.harpapp.ui.screens.settings.SettingsScreen
import com.example.harpapp.ui.screens.song.SongScreen
import com.example.harpapp.ui.screens.splash.SplashScreen
import com.example.harpapp.ui.screens.bluetooth.BluetoothScreen

@Composable
fun AppNavGraph() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.SPLASH) {
            SplashScreen()
        }
        composable(Routes.HOME) {
            HomeScreen()
        }
        composable(Routes.SETTINGS) {
            SettingsScreen()
        }
        composable(Routes.SONG) {
            SongScreen()
        }
        composable(Routes.BLUETOOTH) {
            BluetoothScreen()
        }
    }
}