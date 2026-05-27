package com.example.harpapp.navigation

import android.app.Application
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider

import com.example.harpapp.ui.components.NavBar
import com.example.harpapp.ui.components.TopBar

import com.example.harpapp.ui.screens.bluetooth.BluetoothScreen
import com.example.harpapp.ui.screens.home.HomeScreen
import com.example.harpapp.ui.screens.settings.SettingsScreen
import com.example.harpapp.ui.screens.song.SongScreen
import com.example.harpapp.ui.screens.splash.SplashScreen

import com.example.harpapp.viewmodel.BluetoothViewModel
import com.example.harpapp.viewmodel.HomeViewModel
import com.example.harpapp.viewmodel.SettingsViewModel
import com.example.harpapp.viewmodel.SongViewModel

@Composable
fun AppNavGraph(
    settingsViewModel: SettingsViewModel
) {

    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()

    val currentRoute = navBackStackEntry?.destination?.route

    val normalizedRoute = when {
        currentRoute == Routes.HOME -> Routes.HOME
        currentRoute == Routes.SETTINGS -> Routes.SETTINGS
        currentRoute == Routes.BLUETOOTH -> Routes.BLUETOOTH
        currentRoute?.startsWith("song/") == true || currentRoute == Routes.SONG -> Routes.SONG
        else -> currentRoute
    }

    val context = LocalContext.current
    val homeViewModel: HomeViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(context.applicationContext as Application)
    )

    val bluetoothViewModel: BluetoothViewModel = viewModel()
    val songViewModel: SongViewModel = viewModel()

    val isBluetoothConnected by bluetoothViewModel.isConnected.collectAsState()
    val connectedDeviceName by bluetoothViewModel.connectedDeviceName.collectAsState()

    Scaffold(
        topBar = {

            val title = when (normalizedRoute) {
                Routes.HOME -> "HARPApp"
                Routes.SETTINGS -> "Settings"
                Routes.SONG -> "Song Details"
                Routes.BLUETOOTH -> "Bluetooth"
                else -> "HARPApp"
            }

            if (normalizedRoute != Routes.SPLASH) {
                TopBar(
                    title = title,
                    onBackClick = {
                        if (navController.previousBackStackEntry != null) {
                            navController.popBackStack()
                        }
                    },
                    isBluetoothConnected = isBluetoothConnected,
                    onBluetoothClick = {
                        navController.navigate(Routes.BLUETOOTH)
                    },
                    connectedDeviceName = connectedDeviceName
                )
            }
        },

        bottomBar = {

            if (normalizedRoute != Routes.SPLASH) {
                NavBar(
                    currentRoute = normalizedRoute,
                    onNavigate = { route ->

                        navController.navigate(route) {

                            popUpTo(navController.graph.findStartDestination().id)

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
            modifier = Modifier.padding(innerPadding)
        ) {

            composable(Routes.SPLASH) {
                SplashScreen()
            }

            composable(Routes.HOME) {

                HomeScreen(
                    viewModel = homeViewModel,

                    onNavigateToSong = { songId ->
                        navController.navigate("song/$songId")
                    }
                )
            }

            composable(Routes.SETTINGS) {

                SettingsScreen(
                    viewModel = settingsViewModel,

                    onNavigateToBluetooth = {
                        navController.navigate(Routes.BLUETOOTH)
                    }
                )
            }

            composable(
                route = Routes.SONG,
                arguments = listOf(
                    navArgument("songId") { type = NavType.IntType }
                )
            ) { backStackEntry ->

                val songId = backStackEntry.arguments?.getInt("songId") ?: 0

                SongScreen(
                    songId = songId,
                    viewModel = songViewModel
                )
            }

            composable(Routes.BLUETOOTH) {

                BluetoothScreen(
                    viewModel = bluetoothViewModel
                )
            }
        }
    }
}
