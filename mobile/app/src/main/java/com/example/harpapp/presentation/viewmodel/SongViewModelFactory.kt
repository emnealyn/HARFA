package com.example.harpapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.harpapp.domain.model.ConnectionState
import com.example.harpapp.domain.model.NoteEvent
import com.example.harpapp.domain.repository.AudioPlayer
import com.example.harpapp.domain.usecase.GetSongUseCase
import com.example.harpapp.domain.usecase.InstrumentLessonController
import com.example.harpapp.domain.usecase.PrepareLessonUseCase
import kotlinx.coroutines.flow.Flow

class SongViewModelFactory(
    private val getSongUseCase: GetSongUseCase,
    private val prepareLessonUseCase: PrepareLessonUseCase,
    private val audioPlayer: AudioPlayer,
    private val noteEvents: Flow<NoteEvent>,
    private val connectionState: Flow<ConnectionState>,
    private val lessonControllerFactory: InstrumentLessonController.Factory
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SongViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SongViewModel(
                getSongUseCase = getSongUseCase,
                prepareLessonUseCase = prepareLessonUseCase,
                audioPlayer = audioPlayer,
                noteEvents = noteEvents,
                connectionState = connectionState,
                lessonControllerFactory = lessonControllerFactory
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
