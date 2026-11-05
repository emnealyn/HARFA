package com.example.harpapp.domain.usecase

import com.example.harpapp.domain.model.Difficulty
import com.example.harpapp.domain.model.Song
import org.junit.Assert.assertEquals
import org.junit.Test

class FilterSongsUseCaseTest {

    private val filterSongs = FilterSongsUseCase()

    @Test
    fun filtersBySearchDifficultyAndFavoriteStatus() {
        val matchingSong = song(
            id = 1,
            title = "Morning Song",
            difficulty = Difficulty.EASY,
            isFavorite = true
        )
        val differentDifficulty = song(
            id = 2,
            title = "Morning Song",
            difficulty = Difficulty.HARD,
            isFavorite = true
        )
        val notFavorite = song(
            id = 3,
            title = "Morning Song",
            difficulty = Difficulty.EASY,
            isFavorite = false
        )

        val result = filterSongs(
            songs = listOf(matchingSong, differentDifficulty, notFavorite),
            query = "morning",
            selectedDifficulties = setOf(Difficulty.EASY),
            favoritesOnly = true
        )

        assertEquals(listOf(matchingSong), result)
    }

    @Test
    fun searchesTitlesAndArtistsCaseInsensitively() {
        val titleMatch = song(id = 1, title = "Summer Rain", artist = "Band")
        val artistMatch = song(id = 2, title = "Other Song", artist = "Summer Band")
        val unrelated = song(id = 3, title = "Other Song", artist = "Other Band")

        val result = filterSongs(
            songs = listOf(titleMatch, artistMatch, unrelated),
            query = "SUMMER",
            selectedDifficulties = emptySet(),
            favoritesOnly = false
        )

        assertEquals(listOf(titleMatch, artistMatch), result)
    }

    private fun song(
        id: Int,
        title: String = "Song",
        artist: String = "Artist",
        difficulty: Difficulty = Difficulty.MEDIUM,
        isFavorite: Boolean = false
    ) = Song(
        id = id,
        title = title,
        artist = artist,
        difficulty = difficulty,
        duration = "1:00",
        lyricsWithNotes = emptyList(),
        isFavorite = isFavorite
    )
}
