package com.example.harpapp.domain.usecase

import com.example.harpapp.domain.model.CatalogIssue
import com.example.harpapp.domain.model.CatalogIssueReason
import com.example.harpapp.domain.model.Difficulty
import com.example.harpapp.domain.model.LessonValidation
import com.example.harpapp.domain.model.LyricNote
import com.example.harpapp.domain.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrepareLessonUseCaseTest {

    private val prepareLesson = PrepareLessonUseCase()

    @Test
    fun poprawnyUtworDajeLekcjeZTymiSamymiKrokami() {
        val result = prepareLesson(song(LyricNote("I", "G4"), LyricNote("wan-", "G#4"), LyricNote("na", "C5")))

        assertTrue(result is LessonValidation.Valid)
        val lesson = (result as LessonValidation.Valid).lesson
        assertEquals("7", lesson.songId)
        assertEquals("Test", lesson.title)
        assertEquals(listOf("I", "wan-", "na"), lesson.steps.map { it.syllable })
        assertEquals(listOf(67, 68, 72), lesson.steps.map { it.pitch.midi })
    }

    @Test
    fun pustaINiepoprawnaNutaNiePowodujaAwarii() {
        val result = prepareLesson(song(LyricNote("a", "G4"), LyricNote("b", ""), LyricNote("c", "H4")))

        assertEquals(
            LessonValidation.Invalid(
                listOf(
                    CatalogIssue(1, "", CatalogIssueReason.EMPTY),
                    CatalogIssue(2, "H4", CatalogIssueReason.MALFORMED)
                )
            ),
            result
        )
    }

    @Test
    fun nutaPozaZakresemHarfyBlokujeLekcje() {
        val result = prepareLesson(song(LyricNote("a", "C#5"), LyricNote("b", "D5")))

        assertEquals(
            LessonValidation.Invalid(listOf(CatalogIssue(1, "D5", CatalogIssueReason.OUT_OF_RANGE))),
            result
        )
    }

    @Test
    fun utworBezNutDajeNoSteps() {
        assertEquals(
            LessonValidation.Invalid(listOf(CatalogIssue(null, null, CatalogIssueReason.NO_STEPS))),
            prepareLesson(song())
        )
    }

    private fun song(vararg notes: LyricNote) = Song(
        id = "7",
        title = "Test",
        artist = "Artysta",
        difficulty = Difficulty.EASY,
        duration = "0:10",
        lyricsWithNotes = notes.toList()
    )
}
