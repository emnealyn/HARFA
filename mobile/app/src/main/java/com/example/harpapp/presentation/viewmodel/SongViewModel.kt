package com.example.harpapp.presentation.viewmodel

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.harpapp.domain.model.CatalogIssue
import com.example.harpapp.domain.model.ConnectionState
import com.example.harpapp.domain.model.InstrumentState
import com.example.harpapp.domain.model.Lesson
import com.example.harpapp.domain.model.LessonProgress
import com.example.harpapp.domain.model.LessonStatus
import com.example.harpapp.domain.model.LessonValidation
import com.example.harpapp.domain.model.NoteEvent
import com.example.harpapp.domain.model.Pitch
import com.example.harpapp.domain.model.Song
import com.example.harpapp.domain.model.Tuning
import com.example.harpapp.domain.model.TuningAnalysis
import com.example.harpapp.domain.repository.AudioPlayer
import com.example.harpapp.domain.usecase.AnalyzeTuningUseCase
import com.example.harpapp.domain.usecase.GetSongProgressUseCase
import com.example.harpapp.domain.usecase.GetSongTuningUseCase
import com.example.harpapp.domain.usecase.GetSongUseCase
import com.example.harpapp.domain.usecase.InstrumentLessonController
import com.example.harpapp.domain.usecase.PrepareLessonUseCase
import com.example.harpapp.domain.usecase.SaveSongProgressUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

data class StepTuningHint(
    val pitch: Pitch,
    val analysis: TuningAnalysis
)

class SongViewModel(
    private val getSongUseCase: GetSongUseCase,
    private val prepareLessonUseCase: PrepareLessonUseCase,
    private val getSongProgressUseCase: GetSongProgressUseCase,
    private val saveSongProgressUseCase: SaveSongProgressUseCase,
    private val audioPlayer: AudioPlayer,
    noteEvents: Flow<NoteEvent>,
    connectionState: Flow<ConnectionState>,
    lessonControllerFactory: InstrumentLessonController.Factory,
    instrumentState: Flow<InstrumentState>,
    configurationKnown: Flow<Boolean>,
    private val getSongTuningUseCase: GetSongTuningUseCase,
    private val analyzeTuningUseCase: AnalyzeTuningUseCase
) : ViewModel() {

    private val lessonController =
        lessonControllerFactory.create(viewModelScope) { error ->
            Log.e(TAG, "Nie udało się wysłać komendy lekcji do harfy", error)
        }
    private val _song = mutableStateOf<Song?>(null)
    val song: State<Song?> = _song

    private val _isPlaying = mutableStateOf(false)
    val isPlaying: State<Boolean> = _isPlaying

    private val _progress = mutableStateOf<LessonProgress?>(null)
    val progress: State<LessonProgress?> = _progress

    private val _lessonIssues = mutableStateOf<List<CatalogIssue>>(emptyList())
    val lessonIssues: State<List<CatalogIssue>> = _lessonIssues

    private val _pausedByUser = mutableStateOf(false)

    private var lastConnectionState: ConnectionState = ConnectionState.DISCONNECTED

    val isLearningMode: State<Boolean> = derivedStateOf { _progress.value != null }
    val currentNoteIndex: State<Int> = derivedStateOf { _progress.value?.currentIndex ?: 0 }
    val learningComplete: State<Boolean> = derivedStateOf { _progress.value?.isCompleted == true }

    val isLessonPaused: State<Boolean> =
        derivedStateOf { _progress.value?.status == LessonStatus.PAUSED }

    val isPausedByConnection: State<Boolean> = derivedStateOf {
        _progress.value?.status == LessonStatus.PAUSED && !_pausedByUser.value
    }

    val canToggleLessonPause: State<Boolean> = derivedStateOf {
        when (_progress.value?.status) {
            LessonStatus.IN_PROGRESS -> true
            LessonStatus.PAUSED -> _pausedByUser.value
            LessonStatus.COMPLETED, null -> false
        }
    }

    private data class PendingStart(val lesson: Lesson, val analysis: TuningAnalysis)

    private val _isConnected = mutableStateOf(false)
    private val _instrumentState = mutableStateOf(InstrumentState.DEFAULT)
    private val _configurationKnown = mutableStateOf(false)
    private val _suggestedTuning = mutableStateOf<Tuning?>(null)
    val songTuning: State<Tuning?> = _suggestedTuning
    private val _pendingStart = mutableStateOf<PendingStart?>(null)

    private val tuningIsKnown: Boolean
        get() = _isConnected.value && _configurationKnown.value

    val tuningWarning: State<TuningAnalysis?> = derivedStateOf {
        val song = _song.value
        if (song == null || !tuningIsKnown) {
            null
        } else {
            analyzeTuningUseCase(song, _instrumentState.value, _suggestedTuning.value)
                .takeUnless { it == TuningAnalysis.Compatible }
        }
    }

    val tuningStartWarning: State<TuningAnalysis?> = derivedStateOf { _pendingStart.value?.analysis }

    val currentStepHint: State<StepTuningHint?> = derivedStateOf {
        val step = _progress.value?.takeUnless { it.isCompleted }?.currentStep
        val state = _instrumentState.value
        if (step == null || !tuningIsKnown || state.stringFor(step.pitch) != null) {
            null
        } else {
            analyzeTuningUseCase(listOf(step.pitch), state, _suggestedTuning.value)
                .takeUnless { it == TuningAnalysis.Compatible }
                ?.let { StepTuningHint(step.pitch, it) }
        }
    }

    private val instrumentStateJob: Job = viewModelScope.launch {
        instrumentState.collect { _instrumentState.value = it }
    }

    private val configurationKnownJob: Job = viewModelScope.launch {
        configurationKnown.collect { _configurationKnown.value = it }
    }

    private val noteListenerJob: Job = viewModelScope.launch {
        noteEvents.collect { event ->
            val current = _progress.value ?: return@collect
            val update = current.onNoteEvent(event)
            updateProgress(update.progress)
            lessonController.onUpdate(update)
        }
    }

    private val connectionListenerJob: Job = viewModelScope.launch {
        connectionState.collect { state ->
            lastConnectionState = state
            _isConnected.value = state == ConnectionState.CONNECTED
            val current = _progress.value ?: return@collect
            val newProgress = when (state) {
                ConnectionState.DISCONNECTED -> current.pause()
                ConnectionState.CONNECTED ->
                    if (_pausedByUser.value) current
                    else current.resume().also { lessonController.resume(it) }
                ConnectionState.CONNECTING -> current
            }
            updateProgress(newProgress)
        }
    }

    private fun updateProgress(newProgress: LessonProgress?) {
        _progress.value = newProgress
        newProgress?.let { progress ->
            viewModelScope.launch {
                saveSongProgressUseCase.invoke(progress)
            }
        }
    }

    fun loadSong(songId: String) {
        if (_song.value?.id == songId) return

        viewModelScope.launch {
            val song = getSongUseCase(songId)
            _suggestedTuning.value = song?.let { getSongTuningUseCase(it) }
            _song.value = song
            audioPlayer.release()
            _isPlaying.value = false
            stopLearning()

            val savedProgress = getSongProgressUseCase.invoke(songId)
            if (savedProgress != null && !savedProgress.isCompleted) {
                val restoredProgress = if (lastConnectionState == ConnectionState.CONNECTED) {
                    savedProgress.resume()
                } else {
                    savedProgress.pause()
                }
                updateProgress(restoredProgress)
                if (lastConnectionState == ConnectionState.CONNECTED) {
                    lessonController.resume(restoredProgress)
                }
            }
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
                val warning = tuningWarning.value
                if (warning != null) {
                    _pendingStart.value = PendingStart(validation.lesson, warning)
                } else {
                    beginLesson(validation.lesson)
                }
            }
            is LessonValidation.Invalid -> {
                _lessonIssues.value = validation.issues
                _progress.value = null
            }
        }
    }

    private fun beginLesson(lesson: Lesson) {
        _pausedByUser.value = false
        val progress = LessonProgress.start(lesson)
        if (lastConnectionState == ConnectionState.CONNECTED) {
            updateProgress(progress)
            lessonController.start(progress)
        } else {
            updateProgress(progress.pause())
        }
    }

    fun confirmStartDespiteTuning() {
        val pending = _pendingStart.value ?: return
        _pendingStart.value = null
        beginLesson(pending.lesson)
    }

    fun cancelTuningStart() {
        _pendingStart.value = null
    }

    fun restartLearning() {
        val current = _progress.value ?: return
        beginLesson(current.lesson)
    }

    fun toggleLessonPause() {
        val current = _progress.value ?: return
        when (current.status) {
            LessonStatus.IN_PROGRESS -> {
                _pausedByUser.value = true
                updateProgress(current.pause())
                lessonController.pause()
            }
            LessonStatus.PAUSED -> {
                if (!_pausedByUser.value) return
                _pausedByUser.value = false
                if (lastConnectionState == ConnectionState.CONNECTED) {
                    val resumed = current.resume()
                    updateProgress(resumed)
                    lessonController.resume(resumed)
                }
            }
            LessonStatus.COMPLETED -> Unit
        }
    }

    private fun stopLearning() {
        if (_progress.value != null) lessonController.end()
        _progress.value = null
        _pausedByUser.value = false
        _lessonIssues.value = emptyList()
        _pendingStart.value = null
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
        instrumentStateJob.cancel()
        configurationKnownJob.cancel()
        audioPlayer.release()
        super.onCleared()
    }

    private companion object {
        const val TAG = "SongViewModel"
    }
}
