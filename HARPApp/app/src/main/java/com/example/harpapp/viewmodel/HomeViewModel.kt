package com.example.harpapp.viewmodel

import android.R.attr.query
import androidx.lifecycle.ViewModel
import com.example.harpapp.data.Difficulty
import com.example.harpapp.data.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class HomeViewModel: ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Mock data zanim ogarniemy bazę danych
    private val _allSongs = MutableStateFlow(
        listOf(
            Song(
                id = 1,
                title = "Wildest Dreams",
                artist = "Taylor Swift",
                difficulty = Difficulty.MEDIUM,
                duration = "2:55",
                lyricsWithNotes = "He said, \"Let's get out of this town...\" D D A C D FF#"
            ),
            Song(
                id = 2,
                title = "Let It Go",
                artist = "Idina Menzel",
                difficulty = Difficulty.EASY,
                duration = "3:45",
                lyricsWithNotes = "The snow glows white on the mountain tonight..."
            ),
            Song(
                id = 3,
                title = "My Heart Will Go On",
                artist = "Celine Dion",
                difficulty = Difficulty.HARD,
                duration = "4:20",
                lyricsWithNotes = "Every night in my dreams I see you..."
            ),
            Song(
                id = 4,
                title = "A Thousand Years",
                artist = "Christina Perri",
                difficulty = Difficulty.MEDIUM,
                duration = "4:45",
                lyricsWithNotes = "Heart beats fast, colors and promises..."
            )
        )
    )

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
        initialValue = _allSongs.value
    )

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun playSongPreview(songId: Int) {
        // TO DO: dodać odtwarzanie audio
        println("Playing preview for song $songId")
    }
}