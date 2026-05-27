package com.example.harpapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.harpapp.model.Difficulty
import com.example.harpapp.model.Song
import androidx.compose.ui.tooling.preview.Preview
import com.example.harpapp.ui.theme.HARPAppTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import com.example.harpapp.model.LyricNote


@Composable
fun SongItem(
    song: Song,
    onPreviewClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column (
                modifier = Modifier.weight(1f)
            ){
                Text(
                    text = song.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = song.artist,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .background(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 2.dp)
                ){
                    Text(
                        text = song.difficulty.name,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(start = 16.dp, top = 6.dp)
            ){
                FilledIconButton(
                    onClick = onPreviewClick,
                    modifier = Modifier.size(60.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Preview",
                        modifier = Modifier.size(48.dp)
                    )
                }
                Spacer(modifier = Modifier.height(1.dp))

                Text(
                    text = "preview",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SongItemPreview() {
    HARPAppTheme {
        val testSong = Song(
            id = 1,
            title = "Wildest Dreams",
            artist = "Taylor Swift",
            difficulty = Difficulty.MEDIUM,
            duration = "2:55",
            lyricsWithNotes = listOf(
                LyricNote("I", "G4"),
                LyricNote("wan-", "G4"),
                LyricNote("na", "G4"),
                LyricNote("take", "G4"),
                LyricNote("you", "A4"),
                LyricNote("some-", "B4"),
                LyricNote("where", "G4"),
                LyricNote("so", "G4"),
                LyricNote("you", "G4"),
                LyricNote("know", "C5"),
                LyricNote("I", "C5"),
                LyricNote("care", "B4"),
                LyricNote("But", "G4"),
                LyricNote("it's", "G4"),
                LyricNote("so", "A4"),
                LyricNote("cold", "G4"),
                LyricNote("and", "F#4"),
                LyricNote("I", "F#4"),
                LyricNote("don't", "F#4"),
                LyricNote("know", "F#4"),
                LyricNote("where", "E4")
            )
            )
        SongItem(
            song = testSong,
            onPreviewClick = {}
        )
    }
}
