package com.example.harpapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.harpapp.domain.model.ConnectionState
import com.example.harpapp.domain.model.InstrumentState
import com.example.harpapp.domain.model.NoteEvent
import com.example.harpapp.domain.repository.AudioPlayer
import com.example.harpapp.domain.usecase.AnalyzeTuningUseCase
import com.example.harpapp.domain.usecase.GetSongProgressUseCase
import com.example.harpapp.domain.usecase.GetSongTuningUseCase
import com.example.harpapp.domain.usecase.GetSongUseCase
import com.example.harpapp.domain.usecase.InstrumentLessonController
import com.example.harpapp.domain.usecase.PrepareLessonUseCase
import com.example.harpapp.domain.usecase.SaveSongProgressUseCase
import kotlinx.coroutines.flow.Flow

class SongViewModelFactory(
    private val getSongUseCase: GetSongUseCase,
    private val prepareLessonUseCase: PrepareLessonUseCase,
    private val getSongProgressUseCase: GetSongProgressUseCase,
    private val saveSongProgressUseCase: SaveSongProgressUseCase,
    private val audioPlayer: AudioPlayer,
    private val noteEvents: Flow<NoteEvent>,
    private val connectionState: Flow<ConnectionState>,
    private val lessonControllerFactory: InstrumentLessonController.Factory,
    private val instrumentState: Flow<InstrumentState>,
    private val configurationKnown: Flow<Boolean>,
    private val getSongTuningUseCase: GetSongTuningUseCase,
    private val analyzeTuningUseCase: AnalyzeTuningUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SongViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SongViewModel(
                getSongUseCase = getSongUseCase,
                prepareLessonUseCase = prepareLessonUseCase,
                getSongProgressUseCase = getSongProgressUseCase,
                saveSongProgressUseCase = saveSongProgressUseCase,
                audioPlayer = audioPlayer,
                noteEvents = noteEvents,
                connectionState = connectionState,
                lessonControllerFactory = lessonControllerFactory,
                instrumentState = instrumentState,
                configurationKnown = configurationKnown,
                getSongTuningUseCase = getSongTuningUseCase,
                analyzeTuningUseCase = analyzeTuningUseCase
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
