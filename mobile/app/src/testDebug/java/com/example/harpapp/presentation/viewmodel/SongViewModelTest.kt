package com.example.harpapp.presentation.viewmodel

import com.example.harpapp.device.simulator.FakeInstrumentRepository
import com.example.harpapp.domain.model.Accidental
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
import com.example.harpapp.domain.usecase.GetSongProgressUseCase
import com.example.harpapp.domain.usecase.GetSongTuningUseCase
import com.example.harpapp.domain.usecase.GetSongUseCase
import com.example.harpapp.domain.usecase.InstrumentLessonController
import com.example.harpapp.domain.usecase.PrepareLessonUseCase
import com.example.harpapp.domain.usecase.SaveSongProgressUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
        viewModel = SongViewModel(
            getSongUseCase = GetSongUseCase(songRepository),
            prepareLessonUseCase = PrepareLessonUseCase(),
            getSongProgressUseCase = GetSongProgressUseCase(songRepository),
            saveSongProgressUseCase = SaveSongProgressUseCase(songRepository),
            audioPlayer = SilentAudioPlayer,
            noteEvents = harp.noteEvents,
            connectionState = harp.connectionState,
            lessonControllerFactory = InstrumentLessonController.Factory(harp),
            instrumentState = harp.instrumentState,
            configurationKnown = harp.configurationKnown,
            getSongTuningUseCase = GetSongTuningUseCase(InMemoryTuningRepository),
            analyzeTuningUseCase = AnalyzeTuningUseCase()
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
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
    fun savedProgressIsRestoredOnConnectedHarp() = runTest(dispatcher) {
        startLessonOnConnectedHarp()
        harp.pluck(2)

        viewModel.loadSong(INVALID_SONG_ID)
        harp.clearRecordedCommands()

        viewModel.loadSong(LESSON_SONG_ID)

        assertEquals(LessonStatus.IN_PROGRESS, status)
        assertEquals(1, viewModel.currentNoteIndex.value)
        assertEquals(
            listOf(
                InstrumentCommand.StartLesson(totalSteps = 2),
                InstrumentCommand.LessonStep(index = 1, mask = 1 shl 4)
            ),
            harp.sentCommands
        )
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

        viewModel.toggleLessonPause()
        assertEquals(LessonStatus.PAUSED, status)

        harp.clearRecordedCommands()
        harp.reconnect()

        assertEquals(LessonStatus.IN_PROGRESS, status)
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
    fun restoredLessonShowsBannerWithoutDialog() = runTest(dispatcher) {
        harp.reconnect()
        viewModel.loadSong(TUNING_SONG_ID)
        viewModel.toggleLearningMode()
        viewModel.confirmStartDespiteTuning()

        viewModel.loadSong(INVALID_SONG_ID)
        viewModel.loadSong(TUNING_SONG_ID)

        assertEquals(LessonStatus.IN_PROGRESS, status)
        assertNull(viewModel.tuningStartWarning.value)
        assertNotNull(viewModel.tuningWarning.value)
    }

    private object InMemoryTuningRepository : TuningRepository {
        override suspend fun getTuning(tuningId: String): Tuning? = G_MAJOR.takeIf { it.id == tuningId }
        override suspend fun getTunings(): List<Tuning> = listOf(G_MAJOR)
    }

    private class InMemorySongRepository(private val songs: Map<String, Song>) : SongRepository {
        private val progressMap = mutableMapOf<String, com.example.harpapp.domain.model.LessonProgress>()
        override fun observeSongs(): Flow<List<Song>> = flowOf(songs.values.toList())
        override suspend fun getSong(songId: String): Song? = songs[songId]
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
        override fun toggle(assetPath: String, onCompleted: () -> Unit): Boolean = false
        override fun release() = Unit
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
