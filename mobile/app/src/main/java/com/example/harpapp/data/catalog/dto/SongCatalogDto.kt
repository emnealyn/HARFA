package com.example.harpapp.data.catalog.dto

data class SongCatalogDto(
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val difficulty: String = "EASY",
    val duration: String = "",
    val midiFilePath: String? = null,
    val coverImage: String? = null,
    val lyricsWithNotes: List<LyricNoteDto> = emptyList(),
    val tuningId: String? = null
)
