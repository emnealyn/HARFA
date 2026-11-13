package com.example.harpapp.data.local

import com.example.harpapp.data.catalog.dto.SongCatalogDto
import com.example.harpapp.domain.model.Difficulty
import com.example.harpapp.domain.model.LyricNote
import com.example.harpapp.domain.model.Song

fun SongEntity.toDomain(): Song {
    return Song(
        id = id,
        title = title,
        artist = artist,
        difficulty = Difficulty.valueOf(difficulty),
        duration = duration,
        lyricsWithNotes = lyricsWithNotes.map { LyricNote(it.syllable, it.note) },
        midiFilePath = midiFilePath,
        coverImage = coverImage,
        isFavorite = isFavorite
    )
}

fun Song.toEntity(): SongEntity {
    return SongEntity(
        id = id,
        title = title,
        artist = artist,
        difficulty = difficulty.name,
        duration = duration,
        lyricsWithNotes = lyricsWithNotes.map { LyricNoteEntity(it.syllable, it.note) },
        midiFilePath = midiFilePath,
        coverImage = coverImage,
        isFavorite = isFavorite
    )
}

fun SongCatalogDto.toDomain(): Song {
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

fun SongCatalogDto.toEntity(): SongEntity {
    return SongEntity(
        id = id,
        title = title,
        artist = artist,
        difficulty = difficulty.uppercase(),
        duration = duration,
        lyricsWithNotes = lyricsWithNotes.map { LyricNoteEntity(it.syllable, it.note) },
        midiFilePath = midiFilePath,
        coverImage = coverImage,
        isFavorite = isFavorite
    )
}
