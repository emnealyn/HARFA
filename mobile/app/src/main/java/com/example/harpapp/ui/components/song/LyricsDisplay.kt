package com.example.harpapp.ui.components.song

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.harpapp.model.LyricNote
import com.example.harpapp.model.NoteState

@Composable
fun LyricsDisplay(
    lyrics: List<LyricNote>,
    modifier: Modifier = Modifier,
    currentNoteIndex: Int = -1,
    learningComplete: Boolean = false
) {
    val isLearning = currentNoteIndex >= 0

    Column(modifier = modifier) {
        lyrics.chunked(5).forEachIndexed { chunkIndex, chunk ->
            Row {
                chunk.forEachIndexed { indexInChunk, _ ->
                    val globalIndex = chunkIndex * 5 + indexInChunk
                    val state = when {
                        !isLearning -> NoteState.UPCOMING
                        learningComplete || globalIndex < currentNoteIndex -> NoteState.DONE
                        globalIndex == currentNoteIndex -> NoteState.ACTIVE
                        else -> NoteState.UPCOMING
                    }
                    NoteItem(
                        item = lyrics[globalIndex],
                        state = state
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}