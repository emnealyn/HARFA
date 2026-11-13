package com.example.harpapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val artist: String,
    val difficulty: String,
    val duration: String,
    val lyricsWithNotes: List<LyricNoteEntity>,
    val midiFilePath: String? = null,
    val coverImage: String? = null,
    val isFavorite: Boolean = false
)

data class LyricNoteEntity(
    val syllable: String = "",
    val note: String = ""
)
