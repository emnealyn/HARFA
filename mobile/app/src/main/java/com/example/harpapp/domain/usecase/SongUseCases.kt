package com.example.harpapp.domain.usecase

import com.example.harpapp.domain.model.Difficulty
import com.example.harpapp.domain.model.Song
import com.example.harpapp.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow

class ObserveSongsUseCase(
    private val repository: SongRepository
) {
    operator fun invoke(): Flow<List<Song>> = repository.observeSongs()
}

class GetSongUseCase(
    private val repository: SongRepository
) {
    suspend operator fun invoke(songId: String): Song? = repository.getSong(songId)
}

class SetFavoriteUseCase(
    private val repository: SongRepository
) {
    suspend operator fun invoke(songId: String, isFavorite: Boolean) {
        repository.setFavorite(songId, isFavorite)
    }
}

class FilterSongsUseCase {
    operator fun invoke(
        songs: List<Song>,
        query: String,
        selectedDifficulties: Set<Difficulty>,
        favoritesOnly: Boolean
    ): List<Song> {
        return songs.filter { song ->
            val matchesSearch = song.title.contains(query, ignoreCase = true) ||
                song.artist.contains(query, ignoreCase = true)
            val matchesDifficulty =
                selectedDifficulties.isEmpty() || selectedDifficulties.contains(song.difficulty)
            val matchesFavorites = !favoritesOnly || song.isFavorite

            matchesSearch && matchesDifficulty && matchesFavorites
        }
    }
}
