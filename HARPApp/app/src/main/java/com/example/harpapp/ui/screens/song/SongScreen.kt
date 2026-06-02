package com.example.harpapp.ui.screens.song

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.harpapp.R
import com.example.harpapp.model.Difficulty
import com.example.harpapp.model.LyricNote
import com.example.harpapp.model.Song
import com.example.harpapp.model.getCoverResourceId
import com.example.harpapp.ui.components.home.DifficultyBadge
import com.example.harpapp.ui.components.song.CompletedSongDialog
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
    val isLearningMode by viewModel.isLearningMode
    val currentNoteIndex by viewModel.currentNoteIndex
    val learningComplete by viewModel.learningComplete

    SongContent(
        song = song,
        isPlaying = isPlaying,
        onPlayPauseClick = { viewModel.togglePlayPause() },
        isLearningMode = isLearningMode,
        onLearningModeClick = { viewModel.toggleLearningMode() },
        currentNoteIndex = if (isLearningMode) currentNoteIndex else -1,
        learningComplete = learningComplete,
        onDismissDialog = { viewModel.dismissLearningCompleteDialog() },
        modifier = modifier
    )
}

@Composable
fun SongContent(
    song: Song?,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    isLearningMode: Boolean,
    onLearningModeClick: () -> Unit,
    modifier: Modifier = Modifier,
    currentNoteIndex: Int = -1,
    learningComplete: Boolean = false,
    onDismissDialog: () -> Unit = {},
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (song != null) {
        val imageResId = song.getCoverResourceId()

        if (isLandscape) {
            Row(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier
                            .size(140.dp)
                            .aspectRatio(1f),
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 4.dp
                    ) {
                        Image(
                            painter = painterResource(id = imageResId),
                            contentDescription = "Album Cover",
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DifficultyBadge(difficulty = song.difficulty)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = song.duration,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onLearningModeClick,
                            modifier = Modifier.weight(1f).height(44.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                text = if (isLearningMode) stringResource(R.string.stop_learning)
                                else stringResource(R.string.start_learning),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        
                        OutlinedIconButton(
                            onClick = onPlayPauseClick,
                            modifier = Modifier.size(44.dp),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            colors = IconButtonDefaults.outlinedIconButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1.2f)
                ) {
                    Text(
                        text = stringResource(R.string.lyrics_notes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        )
                    ) {
                        Box(modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                        ) {
                            LyricsDisplay(
                                lyrics = song.lyricsWithNotes,
                                currentNoteIndex = currentNoteIndex,
                                learningComplete = learningComplete
                            )
                        }
                    }
                }
            }
        } else {
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
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DifficultyBadge(difficulty = song.difficulty)
                    Spacer(modifier = Modifier.width(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ){
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = song.duration,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onLearningModeClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text(
                            text = if (isLearningMode) stringResource(R.string.stop_learning)
                            else stringResource(R.string.start_learning),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    OutlinedIconButton(
                        onClick = onPlayPauseClick,
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        colors = IconButtonDefaults.outlinedIconButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause MIDI" else "Play MIDI",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = stringResource(R.string.lyrics_notes),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            LyricsDisplay(
                                lyrics = song.lyricsWithNotes,
                                currentNoteIndex = currentNoteIndex,
                                learningComplete = learningComplete
                            )
                        }
                    }
                }
            }
        }

        if (learningComplete) {
            CompletedSongDialog(onDismiss = onDismissDialog)
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
                onPlayPauseClick = {},
                isLearningMode = true,
                onLearningModeClick = {},
                currentNoteIndex = 2,
                learningComplete = false
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=1280dp,height=800dp,orientation=landscape")
@Composable
fun SongScreenLandscapePreview() {
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
                onPlayPauseClick = {},
                isLearningMode = true,
                onLearningModeClick = {},
                currentNoteIndex = 2,
                learningComplete = false
            )
        }
    }
}
