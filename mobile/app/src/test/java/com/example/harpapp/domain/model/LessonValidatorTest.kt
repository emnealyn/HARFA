package com.example.harpapp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonValidatorTest {

    @Test
    fun `poprawny utwor daje lekcje`() {
        val result = LessonValidator.validate(
            songId = "1",
            title = "Test",
            steps = listOf(CatalogStep("I", "G4"), CatalogStep("wan-", "G#4"), CatalogStep("na", "C5"))
        )

        assertTrue(result is LessonValidation.Valid)
        val lesson = (result as LessonValidation.Valid).lesson
        assertEquals(listOf(67, 68, 72), lesson.steps.map { it.pitch.midi })
        assertEquals(listOf(0, 1, 2), lesson.steps.map { it.index })
        assertEquals("wan-", lesson.steps[1].syllable)
    }

    @Test
    fun `zbiera wszystkie problemy z indeksami krokow`() {
        val result = LessonValidator.validate(
            songId = "1",
            title = "Test",
            steps = listOf(
                CatalogStep("a", "G4"),
                CatalogStep("b", ""),
                CatalogStep("c", "xyz"),
                CatalogStep("d", "E5"),
                CatalogStep("e", null),
                null
            )
        )

        assertEquals(
            LessonValidation.Invalid(
                listOf(
                    CatalogIssue(1, "", CatalogIssueReason.EMPTY),
                    CatalogIssue(2, "xyz", CatalogIssueReason.MALFORMED),
                    CatalogIssue(3, "E5", CatalogIssueReason.OUT_OF_RANGE),
                    CatalogIssue(4, null, CatalogIssueReason.EMPTY),
                    CatalogIssue(5, null, CatalogIssueReason.EMPTY)
                )
            ),
            result
        )
    }

    @Test
    fun `utwor bez nut daje NO_STEPS`() {
        val expected = LessonValidation.Invalid(listOf(CatalogIssue(null, null, CatalogIssueReason.NO_STEPS)))

        assertEquals(expected, LessonValidator.validate("1", "Pusty", emptyList()))
        assertEquals(expected, LessonValidator.validate("1", "Pusty", null))
    }

    @Test
    fun `granice zakresu harfy`() {
        assertTrue(LessonValidator.validate("1", "t", listOf(CatalogStep("", "B0"), CatalogStep("", "C#5"))) is LessonValidation.Valid)
        assertEquals(
            LessonValidation.Invalid(
                listOf(
                    CatalogIssue(0, "Bb0", CatalogIssueReason.OUT_OF_RANGE),
                    CatalogIssue(1, "D5", CatalogIssueReason.OUT_OF_RANGE)
                )
            ),
            LessonValidator.validate("1", "t", listOf(CatalogStep("", "Bb0"), CatalogStep("", "D5")))
        )
    }
}
