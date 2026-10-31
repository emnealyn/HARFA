package com.example.harpapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.harpapp.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import androidx.lifecycle.viewModelScope
import com.example.harpapp.data.AppDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import com.example.harpapp.model.Difficulty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeViewModel(application: Application): AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val songDao = database.songDao()
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    private val _allSongs: Flow<List<Song>> = songDao.getAllSongs()
    private val _selectedDifficulties = MutableStateFlow<Set<Difficulty>>(emptySet())
    val selectedDifficulties = _selectedDifficulties.asStateFlow()
    private val _showFavoritesOnly = MutableStateFlow(false)
    val showFavoritesOnly = _showFavoritesOnly.asStateFlow()


    val filteredSongs = combine(
        _allSongs,
        _searchQuery,
        _selectedDifficulties,
        _showFavoritesOnly
    ) { songs, query, difficulties, favoritesOnly ->
        songs.filter { song ->
            val matchesSearch = song.title.contains(query, ignoreCase = true) ||
                    song.artist.contains(query, ignoreCase = true)
            val matchesDifficulty = difficulties.isEmpty() || difficulties.contains(song.difficulty)
            val matchesFavorites = !favoritesOnly || song.isFavorite

            matchesSearch && matchesDifficulty && matchesFavorites
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun toggleDifficultyFilter(difficulty: Difficulty) {
        val current = _selectedDifficulties.value
        _selectedDifficulties.value = if (current.contains(difficulty)) {
            current - difficulty
        } else {
            current + difficulty
        }
    }

    fun toggleFavorite(songId: Int, currentFavoriteStatus: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            songDao.updateFavoriteStatus(songId, !currentFavoriteStatus)
        }
    }

    fun toggleFavoritesFilter() {
        _showFavoritesOnly.value = !_showFavoritesOnly.value
    }
}
