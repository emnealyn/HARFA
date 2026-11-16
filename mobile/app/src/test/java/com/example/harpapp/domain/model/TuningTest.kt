package com.example.harpapp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TuningTest {

    private fun tuning(vararg changed: Pair<NoteName, Accidental>) = Tuning(
        id = "t",
        name = "T",
        type = TuningType.MAJOR,
        levers = NoteName.entries.associateWith { Accidental.NATURAL } + changed
    )

    private val gMajor = tuning(NoteName.F to Accidental.SHARP)

    @Test
    fun `strojenie pasuje do stanu z tymi samymi dzwigniami niezaleznie od oktawy`() {
        val state = InstrumentState.DEFAULT
            .withLever(NoteName.F, Accidental.SHARP)
            .copy(octaveShift = OctaveShift.UP)

        assertTrue(gMajor.matches(state))
    }

    @Test
    fun `strojenie nie pasuje, gdy jedna dzwignia jest inna`() {
        assertFalse(gMajor.matches(InstrumentState.DEFAULT))
        assertFalse(tuning().matches(InstrumentState.DEFAULT.withLever(NoteName.B, Accidental.FLAT)))
    }

    @Test
    fun `toInstrumentState zachowuje oktawe i zmienia tylko dzwignie`() {
        val base = InstrumentState.DEFAULT.copy(octaveShift = OctaveShift.DOWN)

        val state = gMajor.toInstrumentState(base)

        assertEquals(OctaveShift.DOWN, state.octaveShift)
        assertEquals(Accidental.SHARP, state.lever(NoteName.F))
        assertTrue(gMajor.matches(state))
    }
}
