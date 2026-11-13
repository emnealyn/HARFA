package com.example.harpapp.presentation.viewmodel

import com.example.harpapp.device.simulator.FakeInstrumentRepository
import com.example.harpapp.domain.model.Difficulty
import com.example.harpapp.domain.model.HarpLayout
import com.example.harpapp.domain.model.InstrumentCommand
import com.example.harpapp.domain.model.InstrumentCommand.LessonResult
import com.example.harpapp.domain.model.LessonStatus
import com.example.harpapp.domain.model.LyricNote
import com.example.harpapp.domain.model.Song
import com.example.harpapp.domain.model.StringId
import com.example.harpapp.domain.repository.AudioPlayer
import com.example.harpapp.domain.repository.SongRepository
import com.example.harpapp.domain.usecase.GetSongUseCase
import com.example.harpapp.domain.usecase.InstrumentLessonController
import com.example.harpapp.domain.usecase.PrepareLessonUseCase
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SongViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val harp = FakeInstrumentRepository()
    private lateinit var songs: Map<String, Song>
    private lateinit var viewModel: SongViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        songs = listOf(
            song(LESSON_SONG_ID, noteOn(2), noteOn(4)),
            song(INVALID_SONG_ID, "G4", "H4")
        ).associateBy { it.id }
        viewModel = SongViewModel(
            getSongUseCase = GetSongUseCase(InMemorySongRepository(songs)),
            prepareLessonUseCase = PrepareLessonUseCase(),
            audioPlayer = SilentAudioPlayer,
            noteEvents = harp.noteEvents,
            connectionState = harp.connectionState,
            lessonControllerFactory = InstrumentLessonController.Factory(harp)
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
    fun userPauseEndsLessonOnHarpIgnoresNotesAndResumeSendsCurrentStep() = runTest(dispatcher) {
        startLessonOnConnectedHarp()
        harp.pluck(2)
        harp.clearRecordedCommands()

        viewModel.toggleLessonPause()

        assertEquals(LessonStatus.PAUSED, status)
        assertTrue(viewModel.isLessonPaused.value)
        assertFalse(viewModel.isPausedByConnection.value)
        assertTrue(viewModel.canToggleLessonPause.value)
        assertEquals(listOf(InstrumentCommand.EndLesson), harp.sentCommands)

        harp.pluck(4)
        assertEquals(1, viewModel.currentNoteIndex.value)

        viewModel.toggleLessonPause()

        assertEquals(LessonStatus.IN_PROGRESS, status)
        assertEquals(
            listOf(
                InstrumentCommand.EndLesson,
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
    }
}
