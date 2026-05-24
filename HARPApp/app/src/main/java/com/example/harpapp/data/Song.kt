package com.example.harpapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Difficulty {
    EASY, MEDIUM, HARD
}

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val artist: String,
    val difficulty: Difficulty,
    val duration: String,
    val lyricsWithNotes: String,
    val previewAudio: String? = null
)