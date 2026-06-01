package com.example.harpapp.ui.components.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.harpapp.model.Song

@Composable
fun SongList(
    songs: List<Song>,
    onSongPreviewClick: (Song) -> Unit,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    if (songs.isEmpty()) {
        // TO DO: Obsługa pustej listy
    } else {
        LazyColumn(
            modifier = modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            items(songs) { song ->
                SongItem(
                    song = song,
                    modifier = Modifier.clickable { onSongClick(song) }
                )
            }
        }
    }

}

