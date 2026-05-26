package com.example.harpapp.viewmodel

import android.app.Application
import android.media.MediaPlayer
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.harpapp.data.AppDatabase
import com.example.harpapp.model.Song
import kotlinx.coroutines.launch

class SongViewModel(application: Application) : AndroidViewModel(application) {
    private val songDao = AppDatabase.getDatabase(application, viewModelScope).songDao()
    
    private val _song = mutableStateOf<Song?>(null)
    val song: State<Song?> = _song

    private var mediaPlayer: MediaPlayer? = null
    private val _isPlaying = mutableStateOf(false)
    val isPlaying: State<Boolean> = _isPlaying

    fun loadSong(songId: Int) {
        viewModelScope.launch {
            _song.value = songDao.getSongById(songId)
            // Reset player when switching songs
            mediaPlayer?.release()
            mediaPlayer = null
            _isPlaying.value = false
        }
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
