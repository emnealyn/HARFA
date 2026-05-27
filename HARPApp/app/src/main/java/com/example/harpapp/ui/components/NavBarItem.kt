package com.example.harpapp.ui.components

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.example.harpapp.navigation.Routes

sealed class NavBarItem(
    val route: String,
    val title: String,
    val icon: ImageVector
){
    object Home : NavBarItem(Routes.HOME, "Home", Icons.Default.Home)
    object Song : NavBarItem(Routes.SONG, "Song", Icons.Default.LibraryMusic)
    object Settings : NavBarItem(Routes.SETTINGS, "Settings", Icons.Default.Settings)
}
