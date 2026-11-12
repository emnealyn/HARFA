package com.example.harpapp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InstrumentStateTest {

    private fun string(index: Int): StringId = StringId.of(index)!!

    private fun pitch(raw: String): Pitch = (Pitch.parse(raw) as PitchParseResult.Valid).pitch

    @Test
    fun `stringsOf zwraca wszystkie struny danej nuty`() {
        assertEquals(listOf(0, 7, 14), HarpLayout.stringsOf(NoteName.C).map { it.index })
        assertEquals(listOf(1, 8), HarpLayout.stringsOf(NoteName.D).map { it.index })
        assertEquals(listOf(6, 13), HarpLayout.stringsOf(NoteName.B).map { it.index })
    }

    @Test
    fun `StringId istnieje tylko dla 0 do 14`() {
        assertNull(StringId.of(-1))
        assertNull(StringId.of(15))
        assertEquals(0, StringId.of(0)!!.index)
        assertEquals(14, StringId.of(14)!!.index)
        assertEquals(15, StringId.ALL.size)
    }

    @Test
    fun `strojenie bazowe zgodne z tabela note-mapping 2`() {
        val expectedMidi = listOf(36, 38, 40, 41, 43, 45, 47, 48, 50, 52, 53, 55, 57, 59, 60)
        StringId.ALL.forEach { id ->
            assertEquals("struna ${id.index}", expectedMidi[id.index], InstrumentState.DEFAULT.effectiveMidi(id))
        }
    }

    @Test
    fun `przelacznik A w gore podnosi strune 5 do A#2 (protokol 9_3)`() {
        val state = InstrumentState.DEFAULT.withLever(NoteName.A, Accidental.SHARP)

        assertEquals(46, state.effectiveMidi(string(5)))
        assertEquals(58, state.effectiveMidi(string(12)))
        assertEquals(43, state.effectiveMidi(string(4)))
    }

    @Test
    fun `transpozycja w gore daje G4 na strunie 11`() {
        val state = InstrumentState(octaveShift = OctaveShift.UP)

        assertEquals(67, state.effectiveMidi(string(11)))
    }

    @Test
    fun `stringFor zalezy od strojenia`() {
        assertNull(InstrumentState.DEFAULT.stringFor(pitch("G4")))
        assertEquals(string(11), InstrumentState(octaveShift = OctaveShift.UP).stringFor(pitch("G4")))
        assertEquals(string(4), InstrumentState.DEFAULT.stringFor(pitch("G2")))
        assertNull(InstrumentState.DEFAULT.stringFor(pitch("G#2")))
        assertEquals(
            string(4),
            InstrumentState.DEFAULT.withLever(NoteName.G, Accidental.SHARP).stringFor(pitch("Ab2"))
        )
    }

    @Test
    fun `zakres harfy wynika ze skrajnych ustawien`() {
        val lowest = InstrumentState(
            levers = NoteName.entries.associateWith { Accidental.FLAT },
            octaveShift = OctaveShift.DOWN
        )
        val highest = InstrumentState(
            levers = NoteName.entries.associateWith { Accidental.SHARP },
            octaveShift = OctaveShift.UP
        )

        assertEquals(HarpLayout.MIN_MIDI, StringId.ALL.minOf { lowest.effectiveMidi(it) })
        assertEquals(HarpLayout.MAX_MIDI, StringId.ALL.maxOf { highest.effectiveMidi(it) })
    }
}
