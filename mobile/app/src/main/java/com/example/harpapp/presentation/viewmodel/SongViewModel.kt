package com.example.harpapp.presentation.viewmodel

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.harpapp.data.local.AppDatabase
import com.example.harpapp.domain.model.Song
import com.example.harpapp.domain.model.NotePlayed
import com.example.harpapp.domain.repository.AudioPlayer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SongViewModel(
    application: Application,
    private val audioPlayer: AudioPlayer
) : AndroidViewModel(application) {
    private val songDao = AppDatabase.getDatabase(application, viewModelScope).songDao()

    private val _song = mutableStateOf<Song?>(null)
    val song: State<Song?> = _song

    private val _isPlaying = mutableStateOf(false)
    val isPlaying: State<Boolean> = _isPlaying

    private val _isLearningMode = mutableStateOf(false)
    val isLearningMode: State<Boolean> = _isLearningMode

    // Index of the note the user must play next (-1 = not started / finished)
    private val _currentNoteIndex = mutableStateOf(0)
    val currentNoteIndex: State<Int> = _currentNoteIndex

    // Whether the song has been completed in learning mode
    private val _learningComplete = mutableStateOf(false)
    val learningComplete: State<Boolean> = _learningComplete

    private var noteListenerJob: Job? = null

    fun loadSong(songId: Int) {
        if (_song.value?.id == songId) return

        viewModelScope.launch {
            _song.value = songDao.getSongById(songId)
            audioPlayer.release()
            _isPlaying.value = false
            stopLearning()
        }
    }

    fun attachBluetoothNoteFlow(noteFlow: Flow<NotePlayed>) {
        noteListenerJob?.cancel()
        noteListenerJob = viewModelScope.launch {
            noteFlow.collect { playedNote ->
                if (_isLearningMode.value) {
                    handleIncomingNote(playedNote.note)
                }
            }
        }
    }

    private fun handleIncomingNote(note: String) {
        val lyrics = _song.value?.lyricsWithNotes ?: return
        if (_learningComplete.value) return

        val index = _currentNoteIndex.value
        if (index >= lyrics.size) return

        val expectedNote = lyrics[index].note.trim().uppercase().first().toString()
        if (note.trim().uppercase() == expectedNote) {
            val nextIndex = index + 1
            _currentNoteIndex.value = nextIndex
            if (nextIndex >= lyrics.size) {
                _learningComplete.value = true
            }
        }
    }

    fun toggleLearningMode() {
        if (_isLearningMode.value) {
            stopLearning()
        } else {
            startLearning()
        }
    }

    private fun startLearning() {
        _currentNoteIndex.value = 0
        _learningComplete.value = false
        _isLearningMode.value = true
    }

    private fun stopLearning() {
        _isLearningMode.value = false
        _currentNoteIndex.value = 0
        _learningComplete.value = false
    }

    fun dismissLearningCompleteDialog() {
        _learningComplete.value = false
        stopLearning()
    }

    fun togglePlayPause() {
        val fileName = _song.value?.midiFilePath ?: return
        _isPlaying.value = audioPlayer.toggle(fileName) {
            _isPlaying.value = false
        }
    }

    override fun onCleared() {
        noteListenerJob?.cancel()
        audioPlayer.release()
        super.onCleared()
    }
}
