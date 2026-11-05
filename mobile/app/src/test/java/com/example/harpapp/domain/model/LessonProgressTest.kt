package com.example.harpapp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class LessonProgressTest {

    private val lesson: Lesson = (LessonValidator.validate(
        songId = 1,
        title = "Test",
        steps = listOf(CatalogStep("a", "G4"), CatalogStep("b", "A4"))
    ) as LessonValidation.Valid).lesson

    private fun event(midi: Int) = NoteEvent(StringId.of(11)!!, midi, timestampMs = 0)

    @Test
    fun `trafienie przesuwa krok`() {
        val update = LessonProgress.start(lesson).onNoteEvent(event(67))

        assertEquals(StepOutcome.HIT, update.outcome)
        assertEquals(1, update.progress.currentIndex)
        assertEquals(1, update.progress.hits)
        assertEquals(LessonStatus.IN_PROGRESS, update.progress.status)
    }

    @Test
    fun `pomylka nie przesuwa kroku`() {
        val update = LessonProgress.start(lesson).onNoteEvent(event(60))

        assertEquals(StepOutcome.MISS, update.outcome)
        assertEquals(0, update.progress.currentIndex)
        assertEquals(1, update.progress.misses)
    }

    @Test
    fun `G#4 przy oczekiwanym G4 to pomylka`() {
        assertEquals(StepOutcome.MISS, LessonProgress.start(lesson).onNoteEvent(event(68)).outcome)
    }

    @Test
    fun `ostatnie trafienie konczy lekcje, kolejne zdarzenia sa ignorowane`() {
        val afterFirst = LessonProgress.start(lesson).onNoteEvent(event(67)).progress
        val last = afterFirst.onNoteEvent(event(69))

        assertEquals(StepOutcome.COMPLETE, last.outcome)
        assertEquals(LessonStatus.COMPLETED, last.progress.status)
        assertEquals(2, last.progress.currentIndex)
        assertEquals(2, last.progress.hits)
        assertNull(last.progress.currentStep)

        val after = last.progress.onNoteEvent(event(69))
        assertEquals(StepOutcome.IGNORED, after.outcome)
        assertSame(last.progress, after.progress)
    }

    @Test
    fun `wstrzymana lekcja ignoruje zdarzenia i wraca do tego samego kroku`() {
        val paused = LessonProgress.start(lesson).onNoteEvent(event(67)).progress.pause()

        assertEquals(LessonStatus.PAUSED, paused.status)
        assertEquals(StepOutcome.IGNORED, paused.onNoteEvent(event(69)).outcome)

        val resumed = paused.resume()
        assertEquals(LessonStatus.IN_PROGRESS, resumed.status)
        assertEquals(1, resumed.currentIndex)
        assertEquals(StepOutcome.COMPLETE, resumed.onNoteEvent(event(69)).outcome)
    }
}
