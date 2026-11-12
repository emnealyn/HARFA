package com.example.harpapp.presentation.ui.components

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.example.harpapp.presentation.navigation.Routes
import com.example.harpapp.R

sealed class NavBarItem(
    val route: String,
    val titleResId: Int,
    val icon: ImageVector
){
    object Home : NavBarItem(Routes.HOME, R.string.nav_home, Icons.Default.Home)
    object Song : NavBarItem(Routes.SONG, R.string.nav_song, Icons.Default.LibraryMusic)
    object Harp : NavBarItem(Routes.HARP, R.string.nav_harp, Icons.Default.Piano)
    object Settings : NavBarItem(Routes.SETTINGS, R.string.nav_settings, Icons.Default.Settings)
}
