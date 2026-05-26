package com.example.harpapp.ui.components

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

sealed class NavBarItem(
    val route: String,
    val title: String,
    val icon: ImageVector
){
    object Home : NavBarItem("home", "Home", Icons.Default.Home)
    object Song : NavBarItem("song/1", "Song", Icons.Default.LibraryMusic)
    object Settings : NavBarItem("settings", "Settings", Icons.Default.Settings)
}
