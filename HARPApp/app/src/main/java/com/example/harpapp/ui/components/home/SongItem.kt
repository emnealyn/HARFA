package com.example.harpapp.ui.components.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.harpapp.model.Difficulty
import com.example.harpapp.model.Song
import androidx.compose.ui.tooling.preview.Preview
import com.example.harpapp.ui.theme.HARPAppTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import com.example.harpapp.model.LyricNote
import com.example.harpapp.model.getCoverResourceId


@Composable
fun SongItem(
    song: Song,
    modifier: Modifier = Modifier,
) {
    val difficultyColor = when (song.difficulty) {
        Difficulty.EASY -> Color(0xFF4CAF50)
        Difficulty.MEDIUM -> Color(0xFFFFA726)
        Difficulty.HARD -> Color(0xFFEF5350)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(98.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Cover image
            Image(
                painter = painterResource(id = song.getCoverResourceId()),
                contentDescription = "Cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(98.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                // Title + heart
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = song.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(1.dp))

                // Artist
                Text(
                    text = song.artist,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .wrapContentWidth()
                        .height(24.dp)
                        .background(
                            color = difficultyColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(15.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = song.difficulty.name,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = difficultyColor,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }

            IconButton(
                onClick = { /* TODO: Toggle favorite */ },
                modifier = Modifier
                    .size(48.dp)
                    .padding(end = 24.dp)

            ) {
                Icon(
                    imageVector = if (song.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (song.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
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
            )
        )
        SongItem(
            song = testSong
        )
    }
}