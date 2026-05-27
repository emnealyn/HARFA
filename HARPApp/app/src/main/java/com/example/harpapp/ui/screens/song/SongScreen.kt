package com.example.harpapp.ui.screens.song

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.harpapp.R
import com.example.harpapp.model.Difficulty
import com.example.harpapp.model.LyricNote
import com.example.harpapp.model.Song
import com.example.harpapp.model.getCoverResourceId
import com.example.harpapp.ui.components.song.LyricsDisplay
import com.example.harpapp.ui.theme.HARPAppTheme
import com.example.harpapp.viewmodel.SongViewModel

@Composable
fun SongScreen(
    songId: Int,
    viewModel: SongViewModel,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(songId) {
        viewModel.loadSong(songId)
    }

    val song by viewModel.song
    val isPlaying by viewModel.isPlaying

    SongContent(
        song = song,
        isPlaying = isPlaying,
        onPlayPauseClick = { viewModel.togglePlayPause() },
        modifier = modifier
    )
}

@Composable
fun SongContent(
    song: Song?,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (song != null) {
        val imageResId = song.getCoverResourceId()

        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier
                    .size(220.dp)
                    .aspectRatio(1f),
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 8.dp
            ) {
                Image(
                    painter = painterResource(id = imageResId),
                    contentDescription = "Album Cover",
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = song.title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = song.artist,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Difficulty: ${song.difficulty}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Duration: ${song.duration}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onPlayPauseClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(if (isPlaying) "Pause MIDI" else "Play MIDI")
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Lyrics/Notes:",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                LyricsDisplay(song.lyricsWithNotes)
            }
        }
    } else {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SongScreenPreview() {
    HARPAppTheme {
        val mockSong = Song(
            id = 1,
            title = "Wildest Dreams",
            artist = "Taylor Swift",
            difficulty = Difficulty.MEDIUM,
            duration = "2:55",
            coverImage = "cover_placeholder",
            lyricsWithNotes = listOf(
                LyricNote("He", "D"),
                LyricNote("said", "D"),
                LyricNote("Let's", "A"),
                LyricNote("get", "C"),
                LyricNote("out", "D")
            )
        )
        Surface(color = MaterialTheme.colorScheme.background) {
            SongContent(
                song = mockSong,
                isPlaying = false,
                onPlayPauseClick = {}
            )
        }
    }
}
