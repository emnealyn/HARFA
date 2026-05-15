package com.example.harpapp.ui.components

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

@Composable
fun SongItem(
    song: Song,
    onPreviewClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(modifier = modifier) {
            Column {
                Text(text = song.title)
                Text(text = song.artist)
                Text(text = song.difficulty.name)
            }

            Button(onClick = { /*TODO*/ }) {
                Text(text = "Preview")
            }
        }
    }
}
