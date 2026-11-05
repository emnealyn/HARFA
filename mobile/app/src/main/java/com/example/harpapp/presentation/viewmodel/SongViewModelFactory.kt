package com.example.harpapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.harpapp.domain.model.NotePlayed
import com.example.harpapp.domain.repository.AudioPlayer
import com.example.harpapp.domain.usecase.EvaluateNoteUseCase
import com.example.harpapp.domain.usecase.GetSongUseCase
import kotlinx.coroutines.flow.Flow

class SongViewModelFactory(
    private val getSongUseCase: GetSongUseCase,
    private val evaluateNoteUseCase: EvaluateNoteUseCase,
    private val audioPlayer: AudioPlayer,
    private val incomingNotes: Flow<NotePlayed>
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SongViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SongViewModel(
                getSongUseCase = getSongUseCase,
                evaluateNoteUseCase = evaluateNoteUseCase,
                audioPlayer = audioPlayer,
                incomingNotes = incomingNotes
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
