package com.example.harpapp.data.catalog.dto

import com.google.gson.annotations.SerializedName

data class SongCatalogDto(
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val difficulty: String = "EASY",
    val duration: String = "",
    val midiFilePath: String? = null,
    val coverImage: String? = null,
    @SerializedName("is_favorite")
    val isFavorite: Boolean = false,
    val lyricsWithNotes: List<LyricNoteDto> = emptyList()
)
