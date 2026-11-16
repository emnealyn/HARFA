package com.example.harpapp.domain.model

enum class Difficulty {
    EASY, MEDIUM, HARD
}

data class Song(
    val id: String = "",
    val title: String,
    val artist: String,
    val difficulty: Difficulty,
    val duration: String,
    val lyricsWithNotes: List<LyricNote>,
    val tuningId: String? = null,
    val midiFilePath: String? = null,
    val coverImage: String? = null,
    val isFavorite: Boolean = false
)
