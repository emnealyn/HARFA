package com.example.harpapp.presentation.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.harpapp.presentation.ui.theme.extendedColors

@Composable
fun NavBar(
    currentRoute: String?,
    onNavigate: (route: String) -> Unit,
    modifier: Modifier = Modifier,
    items: List<NavBarItem> = listOf(
        NavBarItem.Home,
        NavBarItem.Song,
        NavBarItem.Harp,
        NavBarItem.Settings

    ),
    alwaysShowLabel: Boolean = false
) {

    val colors = MaterialTheme.colorScheme
    val accent = MaterialTheme.extendedColors.navSelected

    Column(modifier = modifier) {
        HorizontalDivider(thickness = 0.5.dp, color = colors.outlineVariant.copy(alpha = 0.6f))
        NavigationBar(
            containerColor = colors.surface,
            contentColor = colors.onSurfaceVariant,
            tonalElevation = 0.dp
        ) {
            items.forEach { item ->

                val isSongRoute = currentRoute?.startsWith("song/") == true && item is NavBarItem.Song
                val selected = currentRoute == item.route || isSongRoute

                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = stringResource(item.titleResId)
                        )
                    },
                    label = {
                        Text(stringResource(item.titleResId))
                    },
                    selected = selected,
                    onClick = {
                        if (!selected) {
                            onNavigate(item.route)
                        }
                    },
                    alwaysShowLabel = alwaysShowLabel,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = accent,
                        selectedTextColor = accent,
                        indicatorColor = accent.copy(alpha = 0.14f),
                        unselectedIconColor = colors.onSurfaceVariant,
                        unselectedTextColor = colors.onSurfaceVariant
                    )
                )
            }
        }
    }
}
