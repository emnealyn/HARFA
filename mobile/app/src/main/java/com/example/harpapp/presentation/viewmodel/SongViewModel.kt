package com.example.harpapp.presentation.viewmodel

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.harpapp.domain.model.CatalogIssue
import com.example.harpapp.domain.model.ConnectionState
import com.example.harpapp.domain.model.LessonProgress
import com.example.harpapp.domain.model.LessonValidation
import com.example.harpapp.domain.model.NoteEvent
import com.example.harpapp.domain.model.Song
import com.example.harpapp.domain.repository.AudioPlayer
import com.example.harpapp.domain.usecase.GetSongUseCase
import com.example.harpapp.domain.usecase.PrepareLessonUseCase
import com.example.harpapp.domain.usecase.InstrumentLessonController
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class SongViewModel(
    private val getSongUseCase: GetSongUseCase,
    private val prepareLessonUseCase: PrepareLessonUseCase,
    private val audioPlayer: AudioPlayer,
    noteEvents: Flow<NoteEvent>,
    connectionState: Flow<ConnectionState>,
    lessonControllerFactory: InstrumentLessonController.Factory
) : ViewModel() {

    private val lessonController =
    lessonControllerFactory.create(viewModelScope) { error ->
    Log.w(TAG, "Nie udało się wysłać komendy lekcji do harfy", error)
    }
    private val _song = mutableStateOf<Song?>(null)
    val song: State<Song?> = _song

    private val _isPlaying = mutableStateOf(false)
    val isPlaying: State<Boolean> = _isPlaying

    /** Bieżąca lekcja albo `null`, gdy tryb nauki jest wyłączony (docs/domain-model.md §5.2). */
    private val _progress = mutableStateOf<LessonProgress?>(null)
    val progress: State<LessonProgress?> = _progress

    /** Problemy w danych utworu, przez które nie można rozpocząć nauki (docs/domain-model.md §6). */
    private val _lessonIssues = mutableStateOf<List<CatalogIssue>>(emptyList())
    val lessonIssues: State<List<CatalogIssue>> = _lessonIssues

    val isLearningMode: State<Boolean> = derivedStateOf { _progress.value != null }
    val currentNoteIndex: State<Int> = derivedStateOf { _progress.value?.currentIndex ?: 0 }
    val learningComplete: State<Boolean> = derivedStateOf { _progress.value?.isCompleted == true }

    private val noteListenerJob: Job = viewModelScope.launch {
        noteEvents.collect { event ->
            val current = _progress.value ?: return@collect
            val update = current.onNoteEvent(event)
            _progress.value = update.progress
            lessonController.onUpdate(update)
        }
    }

    private val connectionListenerJob: Job = viewModelScope.launch {
        connectionState.collect { state ->
            val current = _progress.value ?: return@collect
            _progress.value = when (state) {
                ConnectionState.DISCONNECTED -> current.pause()
                ConnectionState.CONNECTED -> current.resume().also {
                    lessonController.resume(it)
                }
                ConnectionState.CONNECTING -> current
            }
        }
    }

    fun loadSong(songId: Int) {
        if (_song.value?.id == songId) return

        viewModelScope.launch {
            _song.value = getSongUseCase(songId)
            audioPlayer.release()
            _isPlaying.value = false
            stopLearning()
        }
    }

    fun toggleLearningMode() {
        if (_progress.value != null) {
            stopLearning()
        } else {
            startLearning()
        }
    }

    private fun startLearning() {
        val song = _song.value ?: return
        when (val validation = prepareLessonUseCase(song)) {
            is LessonValidation.Valid -> {
                _lessonIssues.value = emptyList()
                val progress = LessonProgress.start(validation.lesson)
                _progress.value = progress
                lessonController.start(progress)
            }
            is LessonValidation.Invalid -> {
                _lessonIssues.value = validation.issues
                _progress.value = null
            }
        }
    }

    private fun stopLearning() {
        if (_progress.value != null) lessonController.end()
        _progress.value = null
        _lessonIssues.value = emptyList()
    }

    fun dismissLearningCompleteDialog() {
        stopLearning()
    }

    fun dismissLessonIssues() {
        _lessonIssues.value = emptyList()
    }

    fun togglePlayPause() {
        val fileName = _song.value?.midiFilePath ?: return
        _isPlaying.value = audioPlayer.toggle(fileName) {
            _isPlaying.value = false
        }
    }

    override fun onCleared() {
        noteListenerJob.cancel()
        connectionListenerJob.cancel()
        audioPlayer.release()
        super.onCleared()
    }

    private companion object {
        const val TAG = "SongViewModel"
    }
}
