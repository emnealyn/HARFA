package com.example.harpapp.presentation.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.harpapp.R
import com.example.harpapp.presentation.ui.theme.HARPAppTheme

@Composable
fun SongEmptyScreen(
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    SongStatePlaceholder(
        icon = Icons.Default.LibraryMusic,
        title = stringResource(R.string.no_song_selected_title),
        message = stringResource(R.string.no_song_selected_message),
        actionLabel = stringResource(R.string.go_to_home),
        actionIcon = Icons.Default.Home,
        onAction = onNavigateToHome,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun SongEmptyScreenPreview() {
    HARPAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            SongEmptyScreen(onNavigateToHome = {})
        }
    }
}
