package com.example.harpapp.ui.screens.home

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.harpapp.viewmodel.HomeViewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.harpapp.ui.components.SongList
import com.example.harpapp.ui.theme.HARPAppTheme
import android.annotation.SuppressLint
import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.harpapp.data.Difficulty
import com.example.harpapp.data.Song
import com.example.harpapp.ui.theme.HARPAppTheme

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToSong: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val songs by viewModel.filteredSongs.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    HomeContent(
        songs = songs,
        searchQuery = searchQuery,
        onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
        onSongClick = onNavigateToSong,
        onSongPreviewClick = { song -> viewModel.playSongPreview(song.id) },
        modifier = modifier
    )
}

@Composable
fun HomeContent(
    songs: List<Song>,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onSongClick: (Int) -> Unit,
    onSongPreviewClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "What will you play today?",
            fontSize = 25.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(text = "Search for song or artist...") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )

        Text(
            text = "Laser Harp Library",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
        SongList(
            songs = songs,
            onSongPreviewClick = onSongPreviewClick,
            onSongClick = { song -> onSongClick(song.id) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    HARPAppTheme {
        HomeContent(
            songs = listOf(
                Song(1, "Test Song", "Artist", Difficulty.EASY, "3:00", "")
            ),
            searchQuery = "",
            onSearchQueryChanged = {},
            onSongClick = {},
            onSongPreviewClick = {}
        )
    }
}