package com.example.harpapp.presentation.viewmodel

import com.example.harpapp.device.simulator.FakeInstrumentRepository
import com.example.harpapp.domain.model.Accidental
import com.example.harpapp.domain.model.Difficulty
import com.example.harpapp.domain.model.InstrumentState
import com.example.harpapp.domain.model.LessonProgress
import com.example.harpapp.domain.model.LyricNote
import com.example.harpapp.domain.model.NoteName
import com.example.harpapp.domain.model.OctaveShift
import com.example.harpapp.domain.model.Song
import com.example.harpapp.domain.model.Tuning
import com.example.harpapp.domain.model.TuningAnalysis
import com.example.harpapp.domain.model.TuningType
import com.example.harpapp.domain.repository.SongRepository
import com.example.harpapp.domain.repository.TuningRepository
import com.example.harpapp.domain.usecase.AnalyzeTuningUseCase
import com.example.harpapp.domain.usecase.GetSongTuningUseCase
import com.example.harpapp.domain.usecase.GetSongUseCase
import com.example.harpapp.domain.usecase.GetTuningsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HarpViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val harp = FakeInstrumentRepository()
    private lateinit var viewModel: HarpViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        val songs = SongsRepository(listOf(SONG))
        val tunings = TuningsRepository(listOf(C_MAJOR, A_MINOR, G_MAJOR))
        viewModel = HarpViewModel(
            instrumentRepository = harp,
            getSongUseCase = GetSongUseCase(songs),
            getSongTuningUseCase = GetSongTuningUseCase(tunings),
            analyzeTuningUseCase = AnalyzeTuningUseCase(),
            getTuningsUseCase = GetTuningsUseCase(tunings)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.observe() {
        backgroundScope.launch { viewModel.detectedTunings.collect {} }
        backgroundScope.launch { viewModel.target.collect {} }
    }

    @Test
    fun noKeyIsDetectedBeforeFirstConfigState() = runTest(dispatcher) {
        observe()

        assertTrue(viewModel.detectedTunings.value.isEmpty())
    }

    @Test
    fun allTuningsWithMatchingLeversAreDetected() = runTest(dispatcher) {
        observe()
        harp.reconnect()

        assertEquals(listOf(C_MAJOR, A_MINOR), viewModel.detectedTunings.value)

        harp.emitConfiguration(G_MAJOR.toInstrumentState().copy(octaveShift = OctaveShift.UP))

        assertEquals(listOf(G_MAJOR), viewModel.detectedTunings.value)
    }

    @Test
    fun customLeverSettingDetectsNothing() = runTest(dispatcher) {
        observe()
        harp.reconnect()

        harp.emitConfiguration(InstrumentState.DEFAULT.withLever(NoteName.B, Accidental.FLAT))

        assertTrue(viewModel.detectedTunings.value.isEmpty())
    }

    @Test
    fun targetIsShownOnlyAfterSettingSong() = runTest(dispatcher) {
        observe()
        harp.reconnect()

        assertNull(viewModel.target.value)

        viewModel.setTargetSong(SONG.id)
        val target = viewModel.target.value!!
        assertEquals(SONG.title, target.songTitle)
        assertEquals(G_MAJOR.name, target.tuningName)
        val changes = target.analysis as TuningAnalysis.NeedsChanges
        assertEquals(listOf(NoteName.F), changes.levers.map { it.name })
        assertEquals(OctaveShift.UP, changes.octave)

        viewModel.setTargetSong(null)
        assertNull(viewModel.target.value)
    }

    @Test
    fun targetBecomesCompatibleAfterRetuning() = runTest(dispatcher) {
        observe()
        harp.reconnect()
        viewModel.setTargetSong(SONG.id)

        harp.emitConfiguration(G_MAJOR.toInstrumentState().copy(octaveShift = OctaveShift.UP))

        assertEquals(TuningAnalysis.Compatible, viewModel.target.value?.analysis)
    }

    @Test
    fun targetHasNoAnalysisWithoutKnownConfiguration() = runTest(dispatcher) {
        observe()
        viewModel.setTargetSong(SONG.id)

        val target = viewModel.target.value!!
        assertEquals(SONG.title, target.songTitle)
        assertNull(target.analysis)
    }

    private class SongsRepository(private val songs: List<Song>) : SongRepository {
        override fun observeSongs(): Flow<List<Song>> = flowOf(songs)
        override suspend fun getSong(songId: String): Song? = songs.firstOrNull { it.id == songId }
        override suspend fun setFavorite(songId: String, isFavorite: Boolean) = Unit
        override fun observeProgress(songId: String): Flow<LessonProgress?> = flowOf(null)
        override suspend fun getProgress(songId: String): LessonProgress? = null
        override suspend fun saveProgress(progress: LessonProgress) = Unit
        override suspend fun deleteProgress(songId: String) = Unit
    }

    private class TuningsRepository(private val tunings: List<Tuning>) : TuningRepository {
        override suspend fun getTuning(tuningId: String): Tuning? = tunings.firstOrNull { it.id == tuningId }
        override suspend fun getTunings(): List<Tuning> = tunings
    }

    private companion object {
        fun tuning(id: String, name: String, type: TuningType, vararg changed: Pair<NoteName, Accidental>) =
            Tuning(id, name, type, NoteName.entries.associateWith { Accidental.NATURAL } + changed)

        val C_MAJOR = tuning("c-major", "C-dur", TuningType.MAJOR)
        val A_MINOR = tuning("a-minor", "A-moll", TuningType.MINOR)
        val G_MAJOR = tuning("g-major", "G-dur", TuningType.MAJOR, NoteName.F to Accidental.SHARP)

        val SONG = Song(
            id = "another-love",
            title = "Another Love",
            artist = "Tom Odell",
            difficulty = Difficulty.MEDIUM,
            duration = "0:53",
            lyricsWithNotes = listOf("E4", "F#4", "G4", "A4", "B4", "C5").map { LyricNote("la", it) },
            tuningId = G_MAJOR.id
        )
    }
}
