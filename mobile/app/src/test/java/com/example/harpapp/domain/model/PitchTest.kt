package com.example.harpapp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PitchTest {

    private fun pitch(raw: String): Pitch =
        (Pitch.parse(raw) as PitchParseResult.Valid).pitch

    @Test
    fun `G4, G#4 i G5 sa roznymi wartosciami`() {
        val g4 = pitch("G4")
        val gSharp4 = pitch("G#4")
        val g5 = pitch("G5")

        assertNotEquals(g4, gSharp4)
        assertNotEquals(g4, g5)
        assertNotEquals(gSharp4, g5)
        assertEquals(67, g4.midi)
        assertEquals(68, gSharp4.midi)
        assertEquals(79, g5.midi)
    }

    @Test
    fun `pusta nuta daje EMPTY bez wyjatku`() {
        listOf(null, "", "   ", "\t").forEach { raw ->
            assertEquals("wejście: '$raw'", PitchParseResult.Invalid(PitchError.EMPTY), Pitch.parse(raw))
        }
    }

    @Test
    fun `niepoprawny zapis daje MALFORMED bez wyjatku`() {
        listOf("H4", "G##4", "G", "4G", "G#", "G10", "G-1", "Gx4", "G4#", "#4", "BB4", "G 4", "G9", "G٤")
            .forEach { raw ->
                assertEquals("wejście: '$raw'", PitchParseResult.Invalid(PitchError.MALFORMED), Pitch.parse(raw))
            }
    }

    @Test
    fun `parsowanie obcina biale znaki i ignoruje wielkosc litery nuty`() {
        assertEquals(Pitch(NoteName.G, Accidental.NATURAL, 4), pitch(" g4 "))
        assertEquals(Pitch(NoteName.B, Accidental.FLAT, 3), pitch("bb3"))
        assertEquals(Pitch(NoteName.F, Accidental.SHARP, 4), pitch("F#4"))
    }

    @Test
    fun `wartosci z tabel note-mapping`() {
        assertEquals(23, pitch("B0").midi)
        assertEquals(36, pitch("C2").midi)
        assertEquals(60, pitch("C4").midi)
        assertEquals(73, pitch("C#5").midi)
    }

    @Test
    fun `zapis tekstowy odpowiada notacji z katalogu`() {
        listOf("G4", "G#4", "Bb3", "C#5", "B0").forEach { raw ->
            assertEquals(raw, pitch(raw).toString())
        }
    }

    @Test
    fun `fromMidi zachowuje numer MIDI i uzywa krzyzykow w calym zakresie harfy`() {
        for (midi in HarpLayout.MIN_MIDI..HarpLayout.MAX_MIDI) {
            val p = Pitch.fromMidi(midi)
            assertEquals(midi, p.midi)
            assertTrue(p.accidental != Accidental.FLAT)
        }
        assertEquals("C#5", Pitch.fromMidi(73).toString())
        assertEquals("B0", Pitch.fromMidi(23).toString())
    }

    @Test
    fun `enharmonia nie jest rownoscia`() {
        val aFlat4 = pitch("Ab4")
        val gSharp4 = pitch("G#4")

        assertTrue(aFlat4.isEnharmonicWith(gSharp4))
        assertNotEquals(aFlat4, gSharp4)
        assertFalse(pitch("G4").isEnharmonicWith(gSharp4))
    }
}
