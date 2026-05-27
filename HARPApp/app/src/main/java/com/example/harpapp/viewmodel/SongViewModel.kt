package com.example.harpapp.viewmodel

import android.app.Application
import android.media.MediaPlayer
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.harpapp.data.AppDatabase
import com.example.harpapp.model.Song
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class SongViewModel(application: Application) : AndroidViewModel(application) {
    private val songDao = AppDatabase.getDatabase(application, viewModelScope).songDao()

    private val _song = mutableStateOf<Song?>(null)
    val song: State<Song?> = _song

    private var mediaPlayer: MediaPlayer? = null
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
        viewModelScope.launch {
            _song.value = songDao.getSongById(songId)
            // Reset everything when switching songs
            mediaPlayer?.release()
            mediaPlayer = null
            _isPlaying.value = false
            stopLearning()
        }
    }

    fun attachBluetoothNoteFlow(noteFlow: SharedFlow<String>) {
        viewModelScope.launch {
            noteFlow.collect { note ->
                if (_isLearningMode.value) {
                    handleIncomingNote(note)
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
        val currentSong = _song.value ?: return
        val context = getApplication<Application>().applicationContext
        val fileName = currentSong.midiFilePath ?: return

        if (_isPlaying.value) {
            mediaPlayer?.pause()
            _isPlaying.value = false
        } else {
            if (mediaPlayer == null) {
                try {
                    val afd = context.assets.openFd(fileName)
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                        prepare()
                        setOnCompletionListener {
                            _isPlaying.value = false
                            mediaPlayer?.seekTo(0)
                        }
                    }
                    afd.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                    return
                }
            }
            mediaPlayer?.start()
            _isPlaying.value = true
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}