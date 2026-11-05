package com.example.harpapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.harpapp.domain.model.Difficulty
import com.example.harpapp.domain.model.Song
import com.example.harpapp.domain.usecase.FilterSongsUseCase
import com.example.harpapp.domain.usecase.ObserveSongsUseCase
import com.example.harpapp.domain.usecase.SetFavoriteUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val observeSongsUseCase: ObserveSongsUseCase,
    private val filterSongsUseCase: FilterSongsUseCase,
    private val setFavoriteUseCase: SetFavoriteUseCase
) : ViewModel() {

    private val allSongs: Flow<List<Song>> = observeSongsUseCase()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedDifficulties = MutableStateFlow<Set<Difficulty>>(emptySet())
    val selectedDifficulties: StateFlow<Set<Difficulty>> =
        _selectedDifficulties.asStateFlow()

    private val _showFavoritesOnly = MutableStateFlow(false)
    val showFavoritesOnly: StateFlow<Boolean> = _showFavoritesOnly.asStateFlow()

    val filteredSongs: StateFlow<List<Song>> = combine(
        allSongs,
        searchQuery,
        selectedDifficulties,
        showFavoritesOnly
    ) { songs, query, difficulties, favoritesOnly ->
        filterSongsUseCase(songs, query, difficulties, favoritesOnly)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

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
        viewModelScope.launch {
            setFavoriteUseCase(songId, !currentFavoriteStatus)
        }
    }

    fun toggleFavoritesFilter() {
        _showFavoritesOnly.value = !_showFavoritesOnly.value
    }
}
