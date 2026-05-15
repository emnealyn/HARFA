package com.example.harpapp.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.harpapp.ui.theme.DarkRed
import com.example.harpapp.ui.theme.MediumRed
import com.example.harpapp.ui.theme.LightRed

@Composable
fun NavBar(
    currentRoute: String?,
    onNavigate: (route: String) -> Unit,
    modifier: Modifier = Modifier,
    items: List<NavBarItem> = listOf(
        NavBarItem.Home,
        NavBarItem.Song,
        NavBarItem.Settings
    ),
    alwaysShowLabel: Boolean = false
) {
    NavigationBar(
        modifier = modifier,
        containerColor = DarkRed,
        contentColor = Color.White
    ) {
        items.forEach { item ->
            val selected = currentRoute?.let { cur ->
                cur == item.route || cur.startsWith(item.route)
            } ?: false

            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = selected,
                onClick = { if (!selected) onNavigate(item.route) },
                alwaysShowLabel = alwaysShowLabel,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    indicatorColor = MediumRed,
                    unselectedIconColor = Color.White.copy(alpha = 0.7f),
                    unselectedTextColor = Color.White.copy(alpha = 0.7f)
                )
            )
        }
    }
}