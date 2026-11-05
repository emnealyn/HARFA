package com.example.harpapp.domain.usecase

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EvaluateNoteUseCaseTest {

    private val evaluateNote = EvaluateNoteUseCase()

    @Test
    fun treatsCaseAndWhitespaceAsFormatting() {
        assertTrue(evaluateNote("  g4 ", "G4"))
    }

    @Test
    fun distinguishesDifferentOctavesAndAccidentals() {
        assertFalse(evaluateNote("G4", "G5"))
        assertFalse(evaluateNote("G4", "G#4"))
    }
}
