package com.example.harpapp.presentation.viewmodel

import com.example.harpapp.device.simulator.FakeInstrumentRepository
import com.example.harpapp.domain.model.Accidental
import com.example.harpapp.domain.model.AudioPlaybackProgress
import com.example.harpapp.domain.model.AudioPlaybackState
import com.example.harpapp.domain.model.ConnectionState
import com.example.harpapp.domain.model.Difficulty
import com.example.harpapp.domain.model.HarpLayout
import com.example.harpapp.domain.model.InstrumentCommand
import com.example.harpapp.domain.model.InstrumentCommand.LessonResult
import com.example.harpapp.domain.model.InstrumentState
import com.example.harpapp.domain.model.LessonStatus
import com.example.harpapp.domain.model.LyricNote
import com.example.harpapp.domain.model.NoteName
import com.example.harpapp.domain.model.Song
import com.example.harpapp.domain.model.StringId
import com.example.harpapp.domain.model.Tuning
import com.example.harpapp.domain.model.TuningAnalysis
import com.example.harpapp.domain.model.TuningType
import com.example.harpapp.domain.repository.AudioPlayer
import com.example.harpapp.domain.repository.SongRepository
import com.example.harpapp.domain.repository.TuningRepository
import com.example.harpapp.domain.usecase.AnalyzeTuningUseCase
import com.example.harpapp.domain.usecase.DeleteSongProgressUseCase
import com.example.harpapp.domain.usecase.GetSongProgressUseCase
import com.example.harpapp.domain.usecase.GetSongTuningUseCase
import com.example.harpapp.domain.usecase.GetSongUseCase
import com.example.harpapp.domain.usecase.InstrumentLessonController
import com.example.harpapp.domain.usecase.PrepareLessonUseCase
import com.example.harpapp.domain.usecase.SaveSongProgressUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SongViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val harp = FakeInstrumentRepository()
    private lateinit var songs: Map<String, Song>
    private lateinit var songRepository: InMemorySongRepository
    private lateinit var viewModel: SongViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        songs = listOf(
            song(LESSON_SONG_ID, noteOn(2), noteOn(4)),
            song(INVALID_SONG_ID, "G4", "H4"),
            song(TUNING_SONG_ID, "F#2", "G2").copy(tuningId = G_MAJOR.id)
        ).associateBy { it.id }
        songRepository = InMemorySongRepository(songs)
        SilentAudioPlayer.releaseCalls = 0
        SilentAudioPlayer.playbackStateMutable.value = AudioPlaybackState.Idle
        SilentAudioPlayer.playbackProgressMutable.value = AudioPlaybackProgress.EMPTY
        viewModel = SongViewModel(
            getSongUseCase = GetSongUseCase(songRepository),
            prepareLessonUseCase = PrepareLessonUseCase(),
            getSongProgressUseCase = GetSongProgressUseCase(songRepository),
            deleteSongProgressUseCase = DeleteSongProgressUseCase(songRepository),
            saveSongProgressUseCase = SaveSongProgressUseCase(songRepository),
            audioPlayer = SilentAudioPlayer,
            noteEvents = harp.noteEvents,
            connectionState = harp.connectionState,
            lessonControllerFactory = InstrumentLessonController.Factory(harp),
            instrumentState = harp.instrumentState,
            configurationKnown = harp.configurationKnown,
            getSongTuningUseCase = GetSongTuningUseCase(InMemoryTuningRepository),
            analyzeTuningUseCase = AnalyzeTuningUseCase(),
            logError = { _, _ -> }
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialStateIsLoading() {
        assertEquals(SongUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun loadingExistingSongProducesReadyState() = runTest(dispatcher) {
        viewModel.loadSong(LESSON_SONG_ID)

        val state = viewModel.uiState.value
        assertTrue(state is SongUiState.Ready)
        assertEquals(LESSON_SONG_ID, (state as SongUiState.Ready).song.id)
    }

    @Test
    fun loadingMissingSongProducesNotFoundState() = runTest(dispatcher) {
        viewModel.loadSong("missing-song")

        assertEquals(SongUiState.NotFound, viewModel.uiState.value)
        assertNull(viewModel.song.value)
    }

    @Test
    fun loadingSongWithRepositoryErrorProducesErrorState() = runTest(dispatcher) {
        val failure = IllegalStateException("catalog unavailable")
        songRepository.getSongFailure = failure

        viewModel.loadSong(LESSON_SONG_ID)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Unexpected state: $state", state is SongUiState.Error)
        assertEquals(failure, (state as SongUiState.Error).cause)
    }

    @Test
    fun audioPlaybackStateIsExposedByViewModel() = runTest(dispatcher) {
        SilentAudioPlayer.playbackStateMutable.value =
            AudioPlaybackState.Playing("midi/test.mid")

        assertTrue(viewModel.isPlaying.value)
        assertFalse(viewModel.isPreparingAudio.value)
        assertNull(viewModel.audioPlaybackError.value)

        val failure = IllegalStateException("audio unavailable")
        SilentAudioPlayer.playbackStateMutable.value =
            AudioPlaybackState.Error("midi/test.mid", failure)

        assertFalse(viewModel.isPlaying.value)
        assertFalse(viewModel.isPreparingAudio.value)
        assertEquals(failure, viewModel.audioPlaybackError.value)
    }

    @Test
    fun startingLessonStopsReferenceTrack() = runTest(dispatcher) {
        harp.reconnect()
        viewModel.loadSong(LESSON_SONG_ID)
        SilentAudioPlayer.playbackStateMutable.value =
            AudioPlaybackState.Playing("midi/test.mid")
        val releaseCallsBeforeStart = SilentAudioPlayer.releaseCalls

        viewModel.toggleLearningMode()

        assertEquals(releaseCallsBeforeStart + 1, SilentAudioPlayer.releaseCalls)
        assertEquals(AudioPlaybackState.Idle, SilentAudioPlayer.playbackStateMutable.value)
    }

    @Test
    fun endingSongSessionPausesLessonAndReleasesPlayer() = runTest(dispatcher) {
        harp.reconnect()
        viewModel.startSession(LESSON_SONG_ID)
        advanceUntilIdle()
        viewModel.toggleLearningMode()
        harp.clearRecordedCommands()
        val releaseCallsBeforeEnd = SilentAudioPlayer.releaseCalls

        viewModel.endSession()
        viewModel.endSession()

        assertEquals(releaseCallsBeforeEnd + 1, SilentAudioPlayer.releaseCalls)
        assertTrue(viewModel.isLearningMode.value)
        assertEquals(LessonStatus.PAUSED, status)
        assertEquals(listOf(InstrumentCommand.PauseLesson(paused = true)), harp.sentCommands)
    }

    @Test
    fun instrumentEventsAreSubscribedOnlyDuringSongSession() = runTest(dispatcher) {
        assertEquals(0, harp.noteEventSubscriptionCount.value)

        harp.reconnect()
        viewModel.startSession(LESSON_SONG_ID)
        advanceUntilIdle()

        assertEquals(1, harp.noteEventSubscriptionCount.value)
        viewModel.toggleLearningMode()
        harp.pluck(2)
        assertEquals(1, viewModel.currentNoteIndex.value)

        viewModel.endSession()
        advanceUntilIdle()

        assertEquals(0, harp.noteEventSubscriptionCount.value)
        harp.pluck(4)
        assertEquals(1, viewModel.currentNoteIndex.value)

        viewModel.startSession(LESSON_SONG_ID)
        advanceUntilIdle()

        assertEquals(1, harp.noteEventSubscriptionCount.value)
        viewModel.endSession()
    }

    private fun noteOn(string: Int): String {
        val pitch = HarpLayout.basePitch(StringId.of(string)!!)
        return "${pitch.name}${pitch.accidental.symbol}${pitch.octave}"
    }

    private fun song(id: String, vararg notes: String) = Song(
        id = id,
        title = "Test $id",
        artist = "Test",
        difficulty = Difficulty.EASY,
        duration = "0:10",
        lyricsWithNotes = notes.map { LyricNote("la", it) }
    )

    private suspend fun startLessonOnConnectedHarp() {
        harp.reconnect()
        viewModel.loadSong(LESSON_SONG_ID)
        viewModel.toggleLearningMode()
    }

    private val status get() = viewModel.progress.value?.status

    @Test
    fun fullLessonSendsProtocolSequenceAndDismissEndsLesson() = runTest(dispatcher) {
        startLessonOnConnectedHarp()

        harp.pluck(9)
        harp.pluck(2)
        harp.pluck(4)

        assertTrue(viewModel.learningComplete.value)
        assertEquals(1, viewModel.progress.value?.misses)
        assertEquals(2, viewModel.progress.value?.hits)

        viewModel.dismissLearningCompleteDialog()

        assertNull(viewModel.progress.value)
        assertFalse(viewModel.isLearningMode.value)
        assertEquals(
            listOf(
                InstrumentCommand.StartLesson(totalSteps = 2),
                InstrumentCommand.LessonStep(index = 0, mask = 1 shl 2),
                LessonResult(LessonResult.MISS),
                LessonResult(LessonResult.HIT),
                InstrumentCommand.LessonStep(index = 1, mask = 1 shl 4),
                LessonResult(LessonResult.COMPLETE),
                InstrumentCommand.EndLesson
            ),
            harp.sentCommands
        )
    }

    @Test
    fun stopLearningSendsEndLessonAndLeavesLearningMode() = runTest(dispatcher) {
        startLessonOnConnectedHarp()
        harp.clearRecordedCommands()

        viewModel.toggleLearningMode()

        assertFalse(viewModel.isLearningMode.value)
        assertEquals(listOf(InstrumentCommand.EndLesson), harp.sentCommands)
        assertEquals(0, harp.litStrings.value)
    }

    @Test
    fun restartAfterCompleteStartsFromFirstStep() = runTest(dispatcher) {
        startLessonOnConnectedHarp()
        harp.pluck(2)
        harp.pluck(4)
        harp.clearRecordedCommands()

        viewModel.restartLearning()

        assertEquals(0, viewModel.currentNoteIndex.value)
        assertEquals(LessonStatus.IN_PROGRESS, status)
        assertFalse(viewModel.learningComplete.value)
        assertEquals(
            listOf(
                InstrumentCommand.StartLesson(totalSteps = 2),
                InstrumentCommand.LessonStep(index = 0, mask = 1 shl 2)
            ),
            harp.sentCommands
        )
    }

    @Test
    fun savedProgressIsRestoredOnlyAfterManualStart() = runTest(dispatcher) {
        harp.reconnect()
        viewModel.startSession(LESSON_SONG_ID)
        advanceUntilIdle()
        viewModel.toggleLearningMode()
        harp.pluck(2)
        advanceUntilIdle()

        viewModel.endSession()
        advanceUntilIdle()
        harp.clearRecordedCommands()

        viewModel.startSession(LESSON_SONG_ID)
        advanceUntilIdle()

        assertNull(viewModel.progress.value)
        assertFalse(viewModel.isLearningMode.value)
        assertFalse(viewModel.canToggleLessonPause.value)
        assertTrue(harp.sentCommands.isEmpty())

        viewModel.toggleLearningMode()

        assertEquals(LessonStatus.IN_PROGRESS, status)
        assertEquals(1, viewModel.currentNoteIndex.value)
        assertEquals(
            listOf(
                InstrumentCommand.PauseLesson(paused = false)
            ),
            harp.sentCommands
        )
    }

    @Test
    fun switchingToAnotherSongClearsPreviousLessonProgress() = runTest(dispatcher) {
        harp.reconnect()
        viewModel.startSession(LESSON_SONG_ID)
        advanceUntilIdle()
        viewModel.toggleLearningMode()
        harp.pluck(2)
        advanceUntilIdle()

        viewModel.endSession()
        advanceUntilIdle()
        viewModel.startSession(TUNING_SONG_ID)
        advanceUntilIdle()
        viewModel.endSession()
        advanceUntilIdle()

        viewModel.startSession(LESSON_SONG_ID)
        advanceUntilIdle()

        assertNull(viewModel.progress.value)

        viewModel.toggleLearningMode()

        assertEquals(0, viewModel.currentNoteIndex.value)
    }

    @Test
    fun userPausePausesLessonOnHarpIgnoresNotesAndResumeContinuesIt() = runTest(dispatcher) {
        startLessonOnConnectedHarp()
        harp.pluck(2)
        harp.clearRecordedCommands()

        viewModel.toggleLessonPause()

        assertEquals(LessonStatus.PAUSED, status)
        assertTrue(viewModel.isLessonPaused.value)
        assertFalse(viewModel.isPausedByConnection.value)
        assertTrue(viewModel.canToggleLessonPause.value)
        assertEquals(listOf(InstrumentCommand.PauseLesson(paused = true)), harp.sentCommands)
        assertEquals(0, harp.litStrings.value)

        harp.pluck(4)
        assertEquals(1, viewModel.currentNoteIndex.value)

        viewModel.toggleLessonPause()

        assertEquals(LessonStatus.IN_PROGRESS, status)
        // Ta sama sesja: harfa pamięta krok, wystarczy LESSON_PAUSE(0)
        assertEquals(
            listOf(
                InstrumentCommand.PauseLesson(paused = true),
                InstrumentCommand.PauseLesson(paused = false)
            ),
            harp.sentCommands
        )
        assertEquals(1 shl 4, harp.litStrings.value)
    }

    @Test
    fun pausingAndResumingLessonStopsReferenceTrack() = runTest(dispatcher) {
        startLessonOnConnectedHarp()

        SilentAudioPlayer.playbackStateMutable.value =
            AudioPlaybackState.Playing("midi/test.mid")
        val releaseCallsBeforePause = SilentAudioPlayer.releaseCalls
        viewModel.toggleLessonPause()

        assertEquals(releaseCallsBeforePause + 1, SilentAudioPlayer.releaseCalls)
        assertEquals(AudioPlaybackState.Idle, SilentAudioPlayer.playbackStateMutable.value)

        SilentAudioPlayer.playbackStateMutable.value =
            AudioPlaybackState.Playing("midi/test.mid")
        val releaseCallsBeforeResume = SilentAudioPlayer.releaseCalls
        viewModel.toggleLessonPause()

        assertEquals(releaseCallsBeforeResume + 1, SilentAudioPlayer.releaseCalls)
        assertEquals(AudioPlaybackState.Idle, SilentAudioPlayer.playbackStateMutable.value)
    }

    @Test
    fun userResumeAfterReconnectStartsLessonInNewSession() = runTest(dispatcher) {
        startLessonOnConnectedHarp()
        harp.pluck(2)
        viewModel.toggleLessonPause()

        harp.simulateConnectionLoss()
        harp.reconnect()
        harp.clearRecordedCommands()
        viewModel.toggleLessonPause()

        assertEquals(LessonStatus.IN_PROGRESS, status)
        assertEquals(
            listOf(
                InstrumentCommand.StartLesson(totalSteps = 2),
                InstrumentCommand.LessonStep(index = 1, mask = 1 shl 4)
            ),
            harp.sentCommands
        )
    }

    @Test
    fun reconnectDoesNotResumeLessonPausedByUser() = runTest(dispatcher) {
        startLessonOnConnectedHarp()
        viewModel.toggleLessonPause()

        harp.simulateConnectionLoss()
        harp.reconnect()
        harp.clearRecordedCommands()

        assertEquals(LessonStatus.PAUSED, status)
        assertFalse(viewModel.isPausedByConnection.value)
        assertTrue(harp.sentCommands.isEmpty())
    }

    @Test
    fun connectionLossPausesLessonAndReconnectResumesFromLastStep() = runTest(dispatcher) {
        startLessonOnConnectedHarp()
        harp.pluck(2)

        harp.simulateConnectionLoss()

        assertEquals(LessonStatus.PAUSED, status)
        assertTrue(viewModel.isPausedByConnection.value)
        assertFalse(viewModel.canToggleLessonPause.value)

        SilentAudioPlayer.playbackStateMutable.value =
            AudioPlaybackState.Playing("midi/test.mid")
        val releaseCallsBeforeReconnect = SilentAudioPlayer.releaseCalls
        viewModel.toggleLessonPause()
        assertEquals(LessonStatus.PAUSED, status)

        harp.clearRecordedCommands()
        harp.reconnect()

        assertEquals(LessonStatus.IN_PROGRESS, status)
        assertEquals(releaseCallsBeforeReconnect + 1, SilentAudioPlayer.releaseCalls)
        assertEquals(AudioPlaybackState.Idle, SilentAudioPlayer.playbackStateMutable.value)
        assertEquals(1, viewModel.currentNoteIndex.value)
        assertFalse(viewModel.isPausedByConnection.value)
        assertEquals(
            listOf(
                InstrumentCommand.StartLesson(totalSteps = 2),
                InstrumentCommand.LessonStep(index = 1, mask = 1 shl 4)
            ),
            harp.sentCommands
        )
    }

    @Test
    fun lessonStartedWithoutConnectionWaitsForHarp() = runTest(dispatcher) {
        viewModel.loadSong(LESSON_SONG_ID)

        viewModel.toggleLearningMode()

        assertEquals(LessonStatus.PAUSED, status)
        assertTrue(viewModel.isPausedByConnection.value)
        assertTrue(harp.sentCommands.isEmpty())

        harp.reconnect()

        assertEquals(LessonStatus.IN_PROGRESS, status)
        assertEquals(
            listOf(
                InstrumentCommand.StartLesson(totalSteps = 2),
                InstrumentCommand.LessonStep(index = 0, mask = 1 shl 2)
            ),
            harp.sentCommands
        )
    }

    @Test
    fun invalidSongShowsIssuesAndDoesNotStartLesson() = runTest(dispatcher) {
        harp.reconnect()
        viewModel.loadSong(INVALID_SONG_ID)

        viewModel.toggleLearningMode()

        assertFalse(viewModel.isLearningMode.value)
        assertEquals(1, viewModel.lessonIssues.value.size)
        assertTrue(harp.sentCommands.isEmpty())

        viewModel.dismissLessonIssues()
        assertTrue(viewModel.lessonIssues.value.isEmpty())
    }

    @Test
    fun tuningWarningIsHiddenWithoutConnection() = runTest(dispatcher) {
        viewModel.loadSong(TUNING_SONG_ID)

        assertNull(viewModel.tuningWarning.value)
    }

    @Test
    fun songTuningIsExposedForKeyLabel() = runTest(dispatcher) {
        viewModel.loadSong(TUNING_SONG_ID)
        assertEquals(G_MAJOR, viewModel.songTuning.value)

        viewModel.loadSong(LESSON_SONG_ID)
        assertNull(viewModel.songTuning.value)
    }

    @Test
    fun tuningWarningIsHiddenBeforeFirstConfigState() = runTest(dispatcher) {
        harp.simulateConnectionState(ConnectionState.CONNECTING)
        harp.simulateConnectionState(ConnectionState.DISCOVERING_SERVICES)
        harp.simulateConnectionState(ConnectionState.HANDSHAKING)
        harp.simulateConnectionState(ConnectionState.CONNECTED)
        viewModel.loadSong(TUNING_SONG_ID)

        assertNull(viewModel.tuningWarning.value)
    }

    @Test
    fun tuningWarningListsChangesAndClearsAfterRetuning() = runTest(dispatcher) {
        harp.reconnect()
        viewModel.loadSong(TUNING_SONG_ID)

        val warning = viewModel.tuningWarning.value as TuningAnalysis.NeedsChanges
        assertEquals(listOf(NoteName.F), warning.levers.map { it.name })
        assertNull(warning.octave)

        harp.emitConfiguration(F_SHARP)

        assertNull(viewModel.tuningWarning.value)
    }

    @Test
    fun manualStartWithWrongTuningAsksBeforeStarting() = runTest(dispatcher) {
        harp.reconnect()
        viewModel.loadSong(TUNING_SONG_ID)
        viewModel.toggleLearningMode()

        assertNotNull(viewModel.tuningStartWarning.value)
        assertNull(viewModel.progress.value)
        assertTrue(harp.sentCommands.none { it is InstrumentCommand.StartLesson })

        viewModel.confirmStartDespiteTuning()

        assertNull(viewModel.tuningStartWarning.value)
        assertEquals(LessonStatus.IN_PROGRESS, status)
        assertEquals(
            listOf(
                InstrumentCommand.StartLesson(totalSteps = 2),
                InstrumentCommand.LessonStep(index = 0, mask = 0)
            ),
            harp.sentCommands
        )
    }

    @Test
    fun cancellingTuningDialogDoesNotStartLesson() = runTest(dispatcher) {
        harp.reconnect()
        viewModel.loadSong(TUNING_SONG_ID)
        viewModel.toggleLearningMode()

        viewModel.cancelTuningStart()

        assertNull(viewModel.tuningStartWarning.value)
        assertNull(viewModel.progress.value)
        assertTrue(harp.sentCommands.isEmpty())
    }

    @Test
    fun stepWithoutStringShowsHintAndRetuningResendsStep() = runTest(dispatcher) {
        harp.reconnect()
        viewModel.loadSong(TUNING_SONG_ID)
        viewModel.toggleLearningMode()
        viewModel.confirmStartDespiteTuning()

        val hint = viewModel.currentStepHint.value
        assertNotNull(hint)
        assertEquals("F#2", hint!!.pitch.toString())
        assertEquals(
            listOf(NoteName.F),
            (hint.analysis as TuningAnalysis.NeedsChanges).levers.map { it.name }
        )

        harp.clearRecordedCommands()
        harp.emitConfiguration(F_SHARP)

        assertNull(viewModel.currentStepHint.value)
        assertEquals(listOf(InstrumentCommand.LessonStep(index = 0, mask = 1 shl 3)), harp.sentCommands)
    }

    @Test
    fun savedProgressDoesNotActivateLessonWhenSongIsReloaded() = runTest(dispatcher) {
        harp.reconnect()
        viewModel.loadSong(TUNING_SONG_ID)
        viewModel.toggleLearningMode()
        viewModel.confirmStartDespiteTuning()

        viewModel.loadSong(INVALID_SONG_ID)
        viewModel.loadSong(TUNING_SONG_ID)

        assertNull(viewModel.progress.value)
        assertFalse(viewModel.isLearningMode.value)
        assertNull(viewModel.tuningStartWarning.value)
        assertNotNull(viewModel.tuningWarning.value)
    }

    private object InMemoryTuningRepository : TuningRepository {
        override suspend fun getTuning(tuningId: String): Tuning? = G_MAJOR.takeIf { it.id == tuningId }
        override suspend fun getTunings(): List<Tuning> = listOf(G_MAJOR)
    }

    private class InMemorySongRepository(private val songs: Map<String, Song>) : SongRepository {
        private val progressMap = mutableMapOf<String, com.example.harpapp.domain.model.LessonProgress>()
        var getSongFailure: Throwable? = null

        override fun observeSongs(): Flow<List<Song>> = flowOf(songs.values.toList())
        override suspend fun getSong(songId: String): Song? {
            getSongFailure?.let { throw it }
            return songs[songId]
        }
        override suspend fun setFavorite(songId: String, isFavorite: Boolean) = Unit
        override fun observeProgress(songId: String): Flow<com.example.harpapp.domain.model.LessonProgress?> = flowOf(progressMap[songId])
        override suspend fun getProgress(songId: String): com.example.harpapp.domain.model.LessonProgress? = progressMap[songId]
        override suspend fun saveProgress(progress: com.example.harpapp.domain.model.LessonProgress) {
            progressMap[progress.lesson.songId] = progress
        }
        override suspend fun deleteProgress(songId: String) {
            progressMap.remove(songId)
        }
    }

    private object SilentAudioPlayer : AudioPlayer {
        var releaseCalls = 0
        val playbackStateMutable = MutableStateFlow<AudioPlaybackState>(
            AudioPlaybackState.Idle
        )
        val playbackProgressMutable = MutableStateFlow(AudioPlaybackProgress.EMPTY)
        override val playbackState: StateFlow<AudioPlaybackState> = playbackStateMutable
        override val playbackProgress: StateFlow<AudioPlaybackProgress> = playbackProgressMutable

        override fun toggle(assetPath: String, onCompleted: () -> Unit): Boolean = false
        override fun seekTo(positionMs: Long): Boolean {
            val current = playbackProgressMutable.value
            val target = if (current.durationMs > 0L) {
                positionMs.coerceIn(0L, current.durationMs)
            } else {
                positionMs.coerceAtLeast(0L)
            }
            playbackProgressMutable.value = current.copy(positionMs = target)
            return true
        }

        override fun release() {
            releaseCalls++
            playbackStateMutable.value = AudioPlaybackState.Idle
            playbackProgressMutable.value = AudioPlaybackProgress.EMPTY
        }
    }

    private companion object {
        const val LESSON_SONG_ID = "1"
        const val INVALID_SONG_ID = "2"
        const val TUNING_SONG_ID = "3"
        val G_MAJOR = Tuning(
            id = "g-major",
            name = "G-dur",
            type = TuningType.MAJOR,
            levers = NoteName.entries.associateWith { Accidental.NATURAL } + (NoteName.F to Accidental.SHARP)
        )
        val F_SHARP: InstrumentState = InstrumentState.DEFAULT.withLever(NoteName.F, Accidental.SHARP)
    }
}
