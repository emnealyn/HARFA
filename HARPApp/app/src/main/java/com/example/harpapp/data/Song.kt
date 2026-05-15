package com.example.harpapp.data

enum class Difficulty {
    EASY, MEDIUM, HARD
}

data class Song(
    val id: Int = 0,
    val title: String,
    val artist: String,
    val difficulty: Difficulty,
    val duration: String,
    val lyricsWithNotes: String,
    val previewAudio: String? = null
)