package com.example.harpapp.presentation.viewmodel

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.harpapp.domain.model.AudioPlaybackState
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
import com.example.harpapp.domain.usecase.DeleteSongProgressUseCase
import com.example.harpapp.domain.usecase.GetSongProgressUseCase
import com.example.harpapp.domain.usecase.GetSongTuningUseCase
import com.example.harpapp.domain.usecase.GetSongUseCase
import com.example.harpapp.domain.usecase.InstrumentLessonController
import com.example.harpapp.domain.usecase.PrepareLessonUseCase
import com.example.harpapp.domain.usecase.SaveSongProgressUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

sealed interface SongUiState {
    data object Loading : SongUiState
    data class Ready(val song: Song) : SongUiState
    data object NotFound : SongUiState
    data class Error(val cause: Throwable) : SongUiState
}

data class StepTuningHint(
    val pitch: Pitch,
    val analysis: TuningAnalysis
)

class SongViewModel(
    private val getSongUseCase: GetSongUseCase,
    private val prepareLessonUseCase: PrepareLessonUseCase,
    private val getSongProgressUseCase: GetSongProgressUseCase,
    private val deleteSongProgressUseCase: DeleteSongProgressUseCase,
    private val saveSongProgressUseCase: SaveSongProgressUseCase,
    private val audioPlayer: AudioPlayer,
    private val noteEvents: Flow<NoteEvent>,
    private val connectionState: Flow<ConnectionState>,
    lessonControllerFactory: InstrumentLessonController.Factory,
    private val instrumentState: Flow<InstrumentState>,
    private val configurationKnown: Flow<Boolean>,
    private val getSongTuningUseCase: GetSongTuningUseCase,
    private val analyzeTuningUseCase: AnalyzeTuningUseCase,
    private val logError: (String, Throwable) -> Unit = { message, error ->
        Log.e(TAG, message, error)
    }
) : ViewModel() {

    private val _uiState = mutableStateOf<SongUiState>(SongUiState.Loading)
    val uiState: State<SongUiState> = _uiState

    private val lessonController =
        lessonControllerFactory.create(viewModelScope) { error ->
            Log.e(TAG, "Nie udało się wysłać komendy lekcji do harfy", error)
        }
    val song: State<Song?> = derivedStateOf {
        (_uiState.value as? SongUiState.Ready)?.song
    }

    private val _audioPlaybackState = mutableStateOf<AudioPlaybackState>(
        AudioPlaybackState.Idle
    )
    val audioPlaybackState: State<AudioPlaybackState> = _audioPlaybackState
    val isPlaying: State<Boolean> = derivedStateOf {
        _audioPlaybackState.value is AudioPlaybackState.Playing
    }
    val isPreparingAudio: State<Boolean> = derivedStateOf {
        _audioPlaybackState.value is AudioPlaybackState.Preparing
    }
    val audioPlaybackError: State<Throwable?> = derivedStateOf {
        (_audioPlaybackState.value as? AudioPlaybackState.Error)?.cause
    }

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
    private var loadJob: Job? = null
    private var progressSaveJob: Job? = null
    private var sessionTransitionJob: Job? = null
    private var sessionSubscriptionsJob: Job? = null
    private var activeSongId: String? = null
    private var lastSessionSongId: String? = null

    private val currentSong: Song?
        get() = (_uiState.value as? SongUiState.Ready)?.song

    init {
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            audioPlayer.playbackState.collect { state ->
                _audioPlaybackState.value = state
            }
        }
    }

    private val tuningIsKnown: Boolean
        get() = _isConnected.value && _configurationKnown.value

    val tuningWarning: State<TuningAnalysis?> = derivedStateOf {
        val song = currentSong
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

    private fun startSessionSubscriptions() {
        stopSessionSubscriptions()
        sessionSubscriptionsJob = viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            launch(start = CoroutineStart.UNDISPATCHED) {
                instrumentState.collect { _instrumentState.value = it }
            }
            launch(start = CoroutineStart.UNDISPATCHED) {
                configurationKnown.collect { _configurationKnown.value = it }
            }
            launch(start = CoroutineStart.UNDISPATCHED) {
                noteEvents.collect { event ->
                    val current = _progress.value ?: return@collect
                    val update = current.onNoteEvent(event)
                    updateProgress(update.progress)
                    lessonController.onUpdate(update)
                }
            }
            launch(start = CoroutineStart.UNDISPATCHED) {
                connectionState.collect { state ->
                    lastConnectionState = state
                    _isConnected.value = state == ConnectionState.CONNECTED
                    val current = _progress.value ?: return@collect
                    val newProgress = when (state) {
                        ConnectionState.CONNECTED ->
                            if (_pausedByUser.value) current
                            else current.resume().also { lessonController.resume(it) }
                        ConnectionState.SCANNING,
                        ConnectionState.CONNECTING,
                        ConnectionState.DISCOVERING_SERVICES,
                        ConnectionState.HANDSHAKING -> current
                        ConnectionState.BLUETOOTH_DISABLED,
                        ConnectionState.PERMISSION_REQUIRED,
                        ConnectionState.DISCONNECTING,
                        ConnectionState.DISCONNECTED,
                        is ConnectionState.ERROR -> current.pause()
                    }
                    updateProgress(newProgress)
                }
            }
        }
    }

    private fun stopSessionSubscriptions() {
        sessionSubscriptionsJob?.cancel()
        sessionSubscriptionsJob = null
    }

    private fun ensureSessionSubscriptions() {
        if (sessionSubscriptionsJob?.isActive != true) startSessionSubscriptions()
    }

    private fun updateProgress(newProgress: LessonProgress?) {
        _progress.value = newProgress
        newProgress?.let(::persistProgress)
    }

    private fun persistProgress(progress: LessonProgress) {
        progressSaveJob?.cancel()
        progressSaveJob = viewModelScope.launch {
            saveSongProgressUseCase.invoke(progress)
        }
    }

    fun startSession(songId: String) {
        if (activeSongId == songId) {
            ensureSessionSubscriptions()
            return
        }

        val previousSongId = lastSessionSongId
        if (activeSongId != null) endSession()

        activeSongId = songId
        lastSessionSongId = songId
        startSessionSubscriptions()
        sessionTransitionJob?.cancel()
        sessionTransitionJob = viewModelScope.launch {
            if (previousSongId != null && previousSongId != songId) {
                progressSaveJob?.join()
                deleteSongProgressUseCase(previousSongId)
                lessonController.end()
            }
            if (activeSongId == songId) {
                loadSong(songId, forceReload = true)
            }
        }
    }

    fun endSession() {
        val hadActiveSession = activeSongId != null || sessionSubscriptionsJob?.isActive == true
        if (!hadActiveSession) return

        activeSongId = null
        sessionTransitionJob?.cancel()
        stopSessionSubscriptions()
        loadJob?.cancel()
        loadJob = null
        val current = _progress.value
        if (current != null && !current.isCompleted) {
            _pausedByUser.value = true
            updateProgress(current.pause())
            lessonController.pause()
        } else {
            _pausedByUser.value = false
        }
        _lessonIssues.value = emptyList()
        _pendingStart.value = null
        audioPlayer.release()
    }

    fun loadSong(songId: String) {
        ensureSessionSubscriptions()
        loadSong(songId, forceReload = false)
    }

    private fun loadSong(songId: String, forceReload: Boolean) {
        if (!forceReload && (uiState.value as? SongUiState.Ready)?.song?.id == songId) return

        loadJob?.cancel()
        _uiState.value = SongUiState.Loading
        audioPlayer.release()
        clearLessonState()
        _suggestedTuning.value = null

        loadJob = viewModelScope.launch {
            try {
                val song = getSongUseCase(songId)
                if (song == null) {
                    _uiState.value = SongUiState.NotFound
                    return@launch
                }

                val suggestedTuning = getSongTuningUseCase(song)
                _suggestedTuning.value = suggestedTuning

                val savedProgress = getSongProgressUseCase.invoke(songId)
                if (savedProgress != null && !savedProgress.isCompleted) {
                    _pausedByUser.value = true
                    updateProgress(savedProgress.pause())
                }

                _uiState.value = SongUiState.Ready(song)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.value = SongUiState.Error(exception)
                logError("Nie udało się załadować utworu $songId", exception)
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
        val song = currentSong ?: return
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
        val restored = _progress.value
            ?.takeIf { it.lesson.songId == lesson.songId && !it.isCompleted }
        _pausedByUser.value = false
        val progress = restored?.resume() ?: LessonProgress.start(lesson)
        if (lastConnectionState == ConnectionState.CONNECTED) {
            updateProgress(progress)
            if (restored == null) {
                lessonController.start(progress)
            } else {
                lessonController.resume(progress)
            }
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
        clearLessonState()
    }

    private fun clearLessonState() {
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
        val fileName = currentSong?.midiFilePath ?: return
        audioPlayer.toggle(fileName) {}
    }

    override fun onCleared() {
        endSession()
        sessionTransitionJob?.cancel()
        super.onCleared()
    }

    private companion object {
        const val TAG = "SongViewModel"
    }
}
