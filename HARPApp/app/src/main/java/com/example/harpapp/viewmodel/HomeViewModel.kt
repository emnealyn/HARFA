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

class HomeViewModel(application: Application): AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val songDao = database.songDao()
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    private val _allSongs: Flow<List<Song>> = songDao.getAllSongs()
    private val _selectedDifficulties = MutableStateFlow<Set<Difficulty>>(emptySet())
    val selectedDifficulties = _selectedDifficulties.asStateFlow()

    val filteredSongs = combine(_allSongs, _searchQuery, _selectedDifficulties) { songs, query, difficulties ->
        songs.filter { song ->
            val matchesSearch = song.title.contains(query, ignoreCase = true) ||
                    song.artist.contains(query, ignoreCase = true)

            val matchesDifficulty = difficulties.isEmpty() || difficulties.contains(song.difficulty)

            matchesSearch && matchesDifficulty
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

    fun playSongPreview(songId: Int) {
        // TO DO: dodać odtwarzanie audio
        println("Playing preview for song $songId")
    }
}
