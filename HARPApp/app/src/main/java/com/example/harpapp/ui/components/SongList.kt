package com.example.harpapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.harpapp.data.Song
import androidx.compose.ui.tooling.preview.Preview
import com.example.harpapp.ui.theme.HARPAppTheme

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
                    onPreviewClick = { onSongPreviewClick(song) },
                    modifier = Modifier.clickable { onSongClick(song) }
                )
            }
        }
    }

}

// Pierwszy podgląd: Gdy na liście są piosenki
@Preview(showBackground = true)
@Composable
fun SongListNormalPreview() {
    HARPAppTheme {
        val testSongs = listOf(
            com.example.harpapp.data.Song(1, "Wildest Dreams", "Taylor Swift", com.example.harpapp.data.Difficulty.MEDIUM, "2:55", ""),
            com.example.harpapp.data.Song(2, "Let It Go", "Idina Menzel", com.example.harpapp.data.Difficulty.EASY, "3:45", "")
        )
        SongList(songs = testSongs, onSongPreviewClick = {}, onSongClick = {})
    }
}
