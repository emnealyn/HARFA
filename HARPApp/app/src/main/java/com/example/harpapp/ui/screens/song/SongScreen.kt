package com.example.harpapp.ui.screens.song

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily.Companion.Monospace
import androidx.compose.ui.unit.dp
import com.example.harpapp.ui.components.song.LyricsDisplay
import com.example.harpapp.viewmodel.SongViewModel

@Composable
fun SongScreen(
    songId: Int,
    viewModel: SongViewModel
) {
    LaunchedEffect(songId) {
        viewModel.loadSong(songId)
    }

    val song by viewModel.song
    val isPlaying by viewModel.isPlaying

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp, top=0.dp)
            .verticalScroll(rememberScrollState())
    ) {
        if (song != null) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = song!!.title,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Artist: ${song!!.artist}",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Difficulty: ${song!!.difficulty}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Duration: ${song!!.duration}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { viewModel.togglePlayPause() }) {
                Text(if (isPlaying) "Pause MIDI" else "Play MIDI")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Lyrics/Notes:",
                style = MaterialTheme.typography.titleMedium
            )

            song?.let {
                LyricsDisplay(it.lyricsWithNotes)
            }
        } else {
            Text("Loading song...")
        }
    }
}
