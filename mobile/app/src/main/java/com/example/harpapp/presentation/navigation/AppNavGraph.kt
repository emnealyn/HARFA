package com.example.harpapp.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.harpapp.R
import com.example.harpapp.di.AppContainer
import com.example.harpapp.presentation.ui.components.NavBar
import com.example.harpapp.presentation.ui.components.TopBar
import com.example.harpapp.presentation.ui.screens.BluetoothScreen
import com.example.harpapp.presentation.ui.screens.HomeScreen
import com.example.harpapp.presentation.ui.screens.SettingsScreen
import com.example.harpapp.presentation.ui.screens.SongScreen
import com.example.harpapp.presentation.ui.screens.SplashScreen
import com.example.harpapp.presentation.viewmodel.BluetoothViewModel
import com.example.harpapp.presentation.viewmodel.HomeViewModel
import com.example.harpapp.presentation.viewmodel.SettingsViewModel
import com.example.harpapp.presentation.viewmodel.SongViewModel

@Composable
fun AppNavGraph(
    appContainer: AppContainer,
    settingsViewModel: SettingsViewModel
) {
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val normalizedRoute = when {
        currentRoute == Routes.HOME -> Routes.HOME
        currentRoute == Routes.SETTINGS -> Routes.SETTINGS
        currentRoute == Routes.BLUETOOTH -> Routes.BLUETOOTH
        currentRoute?.startsWith("song/") == true -> Routes.SONG
        else -> currentRoute
    }

    val homeViewModel: HomeViewModel = viewModel(factory = appContainer.homeViewModelFactory)
    val bluetoothViewModel: BluetoothViewModel =
        viewModel(factory = appContainer.bluetoothViewModelFactory)
    val songViewModel: SongViewModel = viewModel(factory = appContainer.songViewModelFactory)

    val isBluetoothConnected by bluetoothViewModel.isConnected.collectAsState()
    val connectedDeviceName by bluetoothViewModel.connectedDeviceName.collectAsState(initial = null)

    Scaffold(
        topBar = {
            val title = when (normalizedRoute) {
                Routes.HOME -> stringResource(R.string.app_name)
                Routes.SETTINGS -> stringResource(R.string.nav_settings)
                Routes.BLUETOOTH -> stringResource(R.string.title_bluetooth)
                Routes.SONG -> stringResource(R.string.title_song_details)
                else -> stringResource(R.string.app_name)
            }

            if (currentRoute != Routes.SPLASH) {
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
            if (currentRoute != Routes.SPLASH) {
                NavBar(
                    currentRoute = normalizedRoute,
                    onNavigate = { targetRoute ->

                        val finalRoute = if (targetRoute == Routes.SONG) {
                            val lastOpenedSongId = songViewModel.song.value?.id
                            if (lastOpenedSongId != null && lastOpenedSongId != 0) {
                                Routes.createSongRoute(lastOpenedSongId)
                            } else {
                                Routes.createSongRoute(1)
                            }
                        } else {
                            targetRoute
                        }

                        navController.navigate(finalRoute) {
                            val startDestinationId = navController.graph.findStartDestination().id

                            popUpTo(startDestinationId) {
                                saveState = (targetRoute == Routes.SETTINGS)
                            }

                            launchSingleTop = true
                            restoreState = (targetRoute == Routes.SETTINGS)

                        }
                    }
                )
            }
        }
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.SPLASH) {
                SplashScreen(onAnimationFinished = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                })
            }

            composable(Routes.HOME) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToSong = { songId ->
                        navController.navigate(Routes.createSongRoute(songId))

                    }
                )
            }

            composable(
                route = Routes.SONG_PATTERN,
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

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateToBluetooth = {
                        navController.navigate(Routes.BLUETOOTH)
                    }
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
