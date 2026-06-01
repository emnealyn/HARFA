package com.example.harpapp.ui.components

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.example.harpapp.navigation.Routes
import com.example.harpapp.R

sealed class NavBarItem(
    val route: String,
    val titleResId: Int,
    val icon: ImageVector
){
    object Home : NavBarItem(Routes.HOME, R.string.nav_home, Icons.Default.Home)
    object Song : NavBarItem(Routes.SONG, R.string.nav_song, Icons.Default.LibraryMusic)
    object Settings : NavBarItem(Routes.SETTINGS, R.string.nav_settings, Icons.Default.Settings)
}
