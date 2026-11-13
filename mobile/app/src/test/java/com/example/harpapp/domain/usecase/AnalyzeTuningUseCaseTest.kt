package com.example.harpapp.domain.usecase

import com.example.harpapp.domain.model.Accidental
import com.example.harpapp.domain.model.Difficulty
import com.example.harpapp.domain.model.InstrumentState
import com.example.harpapp.domain.model.LeverChange
import com.example.harpapp.domain.model.LyricNote
import com.example.harpapp.domain.model.NoteName
import com.example.harpapp.domain.model.OctaveShift
import com.example.harpapp.domain.model.Pitch
import com.example.harpapp.domain.model.PitchParseResult
import com.example.harpapp.domain.model.Song
import com.example.harpapp.domain.model.Tuning
import com.example.harpapp.domain.model.TuningAnalysis
import com.example.harpapp.domain.model.TuningType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyzeTuningUseCaseTest {

    private val analyze = AnalyzeTuningUseCase()

    private fun p(raw: String): Pitch = (Pitch.parse(raw) as PitchParseResult.Valid).pitch

    private fun pitches(vararg raw: String): List<Pitch> = raw.map(::p)

    private fun tuning(vararg changed: Pair<NoteName, Accidental>) = Tuning(
        id = "t",
        name = "T",
        type = TuningType.MAJOR,
        levers = NoteName.entries.associateWith { Accidental.NATURAL } + changed
    )

    private val abMajor = tuning(
        NoteName.D to Accidental.FLAT,
        NoteName.E to Accidental.FLAT,
        NoteName.A to Accidental.FLAT,
        NoteName.B to Accidental.FLAT
    )

    private val gMajor = tuning(NoteName.F to Accidental.SHARP)

    private val wildestDreams =pitches("D#4", "F4", "G4", "G#4", "A#4", "C5")

    private fun assertPlayable(state: InstrumentState, notes: List<Pitch>) {
        notes.forEach { assertNotNull("$it", state.stringFor(it)) }
    }

    @Test
    fun `strojenie naturalne jest zgodne z nutami bez znakow`() {
        val result = analyze(pitches("C3", "D3", "E3", "F3", "G3", "A3", "B3"), InstrumentState.DEFAULT, null)

        assertEquals(TuningAnalysis.Compatible, result)
    }

    @Test
    fun `jeden przelacznik`() {
        val result = analyze(pitches("F#3", "G3"), InstrumentState.DEFAULT, gMajor)

        assertEquals(
            TuningAnalysis.NeedsChanges(
                levers = listOf(LeverChange(NoteName.F, Accidental.NATURAL, Accidental.SHARP)),
                octave = null,
                target = InstrumentState.DEFAULT.withLever(NoteName.F, Accidental.SHARP)
            ),
            result
        )
    }

    @Test
    fun `oktawa`() {
        val result = analyze(pitches("G4"), InstrumentState.DEFAULT, tuning())

        result as TuningAnalysis.NeedsChanges
        assertTrue(result.levers.isEmpty())
        assertEquals(OctaveShift.UP, result.octave)
    }

    @Test
    fun `konflikt gdy ten sam stopien jest potrzebny w dwoch pozycjach`() {
        val notes = pitches("E3", "F3", "F#3", "G3")

        val result = analyze(notes, InstrumentState.DEFAULT, null)

        result as TuningAnalysis.Conflict
        assertTrue(result.conflicts.isNotEmpty())
        assertTrue(result.conflicts.all { it.pitches.size >= 2 })
        assertTrue(result.conflicts.flatMap { it.pitches }.all { it in notes })
    }

    @Test
    fun `poza zakresem harfy`() {
        val result = analyze(pitches("A4", "D5", "E5", "F#5"), InstrumentState.DEFAULT, null)

        assertEquals(TuningAnalysis.OutOfRange(pitches("D5", "E5", "F#5")), result)
    }

    @Test
    fun `enharmonia - G#4 zagrane jako Ab bez zmiany struny G`() {
        val state = abMajor.toInstrumentState().copy(octaveShift = OctaveShift.UP)

        assertEquals(TuningAnalysis.Compatible, analyze(wildestDreams, state, null))
        assertEquals(TuningAnalysis.Compatible, analyze(pitches("G4", "Ab4"), state, null))
    }

    @Test
    fun `sugestia wygrywa i zmienia tylko uzywane przelaczniki`() {
        val result = analyze(wildestDreams, InstrumentState.DEFAULT, abMajor)

        result as TuningAnalysis.NeedsChanges
        assertEquals(
            listOf(
                LeverChange(NoteName.E, Accidental.NATURAL, Accidental.FLAT),
                LeverChange(NoteName.A, Accidental.NATURAL, Accidental.FLAT),
                LeverChange(NoteName.B, Accidental.NATURAL, Accidental.FLAT)
            ),
            result.levers
        )
        assertEquals(OctaveShift.UP, result.octave)
        assertEquals(Accidental.NATURAL, result.target.lever(NoteName.D))
        assertPlayable(result.target, wildestDreams)
    }

    @Test
    fun `sugestia z doborem oktawy`() {
        val anotherLove = pitches("E4", "F#4", "G4", "A4", "B4", "C5")

        val result = analyze(anotherLove, InstrumentState.DEFAULT, gMajor)

        assertEquals(
            TuningAnalysis.NeedsChanges(
                levers = listOf(LeverChange(NoteName.F, Accidental.NATURAL, Accidental.SHARP)),
                octave = OctaveShift.UP,
                target = InstrumentState.DEFAULT
                    .withLever(NoteName.F, Accidental.SHARP)
                    .copy(octaveShift = OctaveShift.UP)
            ),
            result
        )
    }

    @Test
    fun `niegrywalna sugestia daje niezgodnosc bez podpowiedzi`() {
        val anotherLove = pitches("E4", "F#4", "G4", "A4", "B4", "C5")

        assertEquals(TuningAnalysis.Incompatible, analyze(anotherLove, InstrumentState.DEFAULT, tuning()))
    }

    @Test
    fun `brak sugestii daje niezgodnosc bez podpowiedzi`() {
        assertEquals(TuningAnalysis.Incompatible, analyze(wildestDreams, InstrumentState.DEFAULT, null))
    }

    @Test
    fun `preferowana jest biezaca oktawa`() {
        val state = InstrumentState.DEFAULT.copy(octaveShift = OctaveShift.DOWN)

        val result = analyze(pitches("F#2", "G2"), state, gMajor)

        result as TuningAnalysis.NeedsChanges
        assertEquals(null, result.octave)
        assertEquals(listOf(NoteName.F), result.levers.map { it.name })
    }

    @Test
    fun `utwor - nieprawidlowe nuty sa pomijane`() {
        val song = Song(
            id = "s",
            title = "S",
            artist = "A",
            difficulty = Difficulty.EASY,
            duration = "0:01",
            lyricsWithNotes = listOf(LyricNote("a", "G4"), LyricNote("b", ""), LyricNote("c", "X9"))
        )

        val result = analyze(song, InstrumentState.DEFAULT, tuning())

        result as TuningAnalysis.NeedsChanges
        assertEquals(OctaveShift.UP, result.octave)
        assertFalse(result.levers.isNotEmpty())
    }

    @Test
    fun `pusta lista nut jest zgodna`() {
        assertEquals(TuningAnalysis.Compatible, analyze(emptyList<Pitch>(), InstrumentState.DEFAULT, null))
    }
}
