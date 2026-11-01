package com.example.harpapp.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.harpapp.domain.repository.AudioPlayer

class SongViewModelFactory(
    private val application: Application,
    private val audioPlayer: AudioPlayer
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SongViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SongViewModel(application, audioPlayer) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
