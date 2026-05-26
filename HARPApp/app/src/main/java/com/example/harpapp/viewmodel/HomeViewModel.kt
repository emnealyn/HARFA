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

class HomeViewModel(application: Application): AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val songDao = database.songDao()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _allSongs: Flow<List<Song>> = songDao.getAllSongs()

    val filteredSongs: StateFlow<List<Song>> = combine(_allSongs, _searchQuery) { songs, query ->
        if (query.isBlank()) {
            songs
        } else {
            songs.filter { song ->
                song.title.contains(query, ignoreCase = true) ||
                        song.artist.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun playSongPreview(songId: Int) {
        // TO DO: dodać odtwarzanie audio
        println("Playing preview for song $songId")
    }
}
