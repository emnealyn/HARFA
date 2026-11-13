package com.example.harpapp.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.example.harpapp.presentation.ui.screens.HarpScreen
import com.example.harpapp.presentation.ui.screens.HomeScreen
import com.example.harpapp.presentation.ui.screens.SettingsScreen
import com.example.harpapp.presentation.ui.screens.SongScreen
import com.example.harpapp.presentation.ui.screens.SplashScreen
import com.example.harpapp.presentation.viewmodel.BluetoothViewModel
import com.example.harpapp.presentation.viewmodel.HarpViewModel
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
        currentRoute == Routes.HARP -> Routes.HARP
        currentRoute == Routes.DEV_SIMULATOR -> Routes.DEV_SIMULATOR
        currentRoute?.startsWith("song/") == true -> Routes.SONG
        else -> currentRoute
    }

    val homeViewModel: HomeViewModel = viewModel(factory = appContainer.homeViewModelFactory)
    val bluetoothViewModel: BluetoothViewModel =
        viewModel(factory = appContainer.bluetoothViewModelFactory)
    val songViewModel: SongViewModel = viewModel(factory = appContainer.songViewModelFactory)
    val harpViewModel: HarpViewModel = viewModel(factory = appContainer.harpViewModelFactory)

    val isBluetoothConnected by bluetoothViewModel.isConnected.collectAsState()
    val connectedDeviceName by bluetoothViewModel.connectedDeviceName.collectAsState(initial = null)
    val batteryStatus by bluetoothViewModel.batteryStatus.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val lowBatteryMessage = stringResource(R.string.battery_low_warning)

    LaunchedEffect(lowBatteryMessage) {
        bluetoothViewModel.lowBatteryWarnings.collect {
            snackbarHostState.showSnackbar(lowBatteryMessage)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            val title = when (normalizedRoute) {
                Routes.HOME -> stringResource(R.string.app_name)
                Routes.SETTINGS -> stringResource(R.string.nav_settings)
                Routes.BLUETOOTH -> stringResource(R.string.title_bluetooth)
                Routes.HARP -> stringResource(R.string.title_harp)
                Routes.SONG -> stringResource(R.string.title_song_details)
                Routes.DEV_SIMULATOR -> "Symulator harfy" //to jest tylko w trybie debug więc nie dodawałam tego do string.xml
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
                        navController.navigate(Routes.HARP) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    connectedDeviceName = connectedDeviceName,
                    batteryStatus = batteryStatus
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
                            if (!lastOpenedSongId.isNullOrEmpty()) {
                                Routes.createSongRoute(lastOpenedSongId)
                            } else {
                                Routes.createSongRoute("another-love")
                            }
                        } else {
                            targetRoute
                        }

                        navController.navigate(finalRoute) {
                            val startDestinationId = navController.graph.findStartDestination().id

                            popUpTo(startDestinationId) {
                                saveState = (targetRoute == Routes.SETTINGS || targetRoute == Routes.HARP)
                            }

                            launchSingleTop = true
                            restoreState = (targetRoute == Routes.SETTINGS || targetRoute == Routes.HARP)

                        }
                    }
                )
            }
        }
    ) { innerPadding ->

        Box(modifier = Modifier.padding(innerPadding)) {
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH
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
                    navArgument("songId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val songId = backStackEntry.arguments?.getString("songId").orEmpty()
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
                    },
                    devTools = appContainer.devTools,
                    onNavigateToSimulator = {
                        navController.navigate(Routes.DEV_SIMULATOR)
                    }
                )
            }

            composable(Routes.HARP) {
                HarpScreen(
                    viewModel = harpViewModel,
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

            appContainer.devTools?.let { devTools ->
                composable(Routes.DEV_SIMULATOR) {
                    devTools.SimulatorPanel()
                }
            }
        }

        if (normalizedRoute == Routes.SONG) {
            appContainer.devTools?.FloatingStrings(Modifier.fillMaxSize())
        }
        }
    }
}
