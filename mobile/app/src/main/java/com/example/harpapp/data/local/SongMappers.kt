package com.example.harpapp.data.local

import com.example.harpapp.data.catalog.dto.SongCatalogDto
import com.example.harpapp.domain.model.Difficulty
import com.example.harpapp.domain.model.LyricNote
import com.example.harpapp.domain.model.Song

fun SongCatalogDto.toDomain(isFavorite: Boolean = false): Song {
    return Song(
        id = id,
        title = title,
        artist = artist,
        difficulty = Difficulty.valueOf(difficulty.uppercase()),
        duration = duration,
        lyricsWithNotes = lyricsWithNotes.map { LyricNote(it.syllable, it.note) },
        midiFilePath = midiFilePath,
        coverImage = coverImage,
        isFavorite = isFavorite
    )
}
