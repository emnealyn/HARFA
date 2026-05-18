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
import com.example.harpapp.data.Difficulty
import com.example.harpapp.data.Song
import androidx.compose.ui.tooling.preview.Preview
import com.example.harpapp.ui.theme.DarkRed
import com.example.harpapp.ui.theme.HARPAppTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow


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
        border = BorderStroke(1.dp, DarkRed)
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
                )

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .background(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 2.dp)
                ){
                    Text(
                        text = song.difficulty.name,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(start = 16.dp, top = 6.dp)
            ){
                FilledIconButton(
                    onClick = onPreviewClick,
                    modifier = Modifier.size(60.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Preview",
                        tint = Color.White,
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
            lyricsWithNotes = ""
        )
        SongItem(
            song = testSong,
            onPreviewClick = {}
        )
    }
}
