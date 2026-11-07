package com.example.harpapp.domain.usecase

import com.example.harpapp.device.simulator.FakeInstrumentRepository
import com.example.harpapp.domain.model.HarpLayout
import com.example.harpapp.domain.model.InstrumentCommand
import com.example.harpapp.domain.model.InstrumentCommand.LessonResult
import com.example.harpapp.domain.model.Lesson
import com.example.harpapp.domain.model.LessonProgress
import com.example.harpapp.domain.model.LessonStep
import com.example.harpapp.domain.model.NoteEvent
import com.example.harpapp.domain.model.StringId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InstrumentLessonControllerTest {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val repository = FakeInstrumentRepository()
    private val errors = mutableListOf<Throwable>()
    private val controller = InstrumentLessonController.Factory(repository).create(scope) { errors += it }

    @After
    fun tearDown() = scope.cancel()

    private fun lessonOnStrings(vararg strings: Int) = Lesson(
        songId = 1,
        title = "test",
        steps = strings.mapIndexed { index, s ->
            LessonStep(index, "la", HarpLayout.basePitch(StringId.of(s)!!))
        }
    )

    private fun noteOn(string: Int) = StringId.of(string)!!.let { id ->
        NoteEvent(id, HarpLayout.basePitch(id).midi, timestampMs = 0)
    }

    @Test
    fun startSendsLessonStartAndFirstStepWithStringMask() = runBlocking {
        repository.reconnect()

        controller.start(LessonProgress.start(lessonOnStrings(11, 3)))

        assertEquals(
            listOf(
                InstrumentCommand.StartLesson(totalSteps = 2),
                InstrumentCommand.LessonStep(index = 0, mask = 1 shl 11)
            ),
            repository.sentCommands
        )
        assertEquals(1 shl 11, repository.litStrings.value)
    }

    @Test
    fun hitMissCompleteAndEndProduceProtocolSequence() = runBlocking {
        repository.reconnect()
        var progress = LessonProgress.start(lessonOnStrings(2, 4))
        controller.start(progress)
        repository.clearRecordedCommands()

        progress.onNoteEvent(noteOn(9)).also { controller.onUpdate(it); progress = it.progress } // MISS
        progress.onNoteEvent(noteOn(2)).also { controller.onUpdate(it); progress = it.progress } // HIT
        progress.onNoteEvent(noteOn(4)).also { controller.onUpdate(it); progress = it.progress } // COMPLETE
        controller.end()

        assertEquals(
            listOf(
                LessonResult(LessonResult.MISS),
                LessonResult(LessonResult.HIT),
                InstrumentCommand.LessonStep(index = 1, mask = 1 shl 4),
                LessonResult(LessonResult.COMPLETE),
                InstrumentCommand.EndLesson
            ),
            repository.sentCommands
        )
        assertTrue(errors.isEmpty())
    }

    @Test
    fun sendsNothingWhenDisconnectedAndResumesAfterReconnect() = runBlocking {
        var progress = LessonProgress.start(lessonOnStrings(0, 5))
        repository.reconnect()
        progress.onNoteEvent(noteOn(0)).also { progress = it.progress } // krok 1 przed utratą

        repository.simulateConnectionLoss()
        controller.start(progress)
        assertTrue(repository.sentCommands.isEmpty())

        repository.reconnect()
        controller.resume(progress)

        assertEquals(
            listOf(
                InstrumentCommand.StartLesson(totalSteps = 2),
                InstrumentCommand.LessonStep(index = 1, mask = 1 shl 5)
            ),
            repository.sentCommands
        )
    }

    @Test
    fun sendFailureIsReportedAndDoesNotStopQueue() = runBlocking {
        repository.reconnect()
        repository.failNextCommand(IllegalStateException("timeout"))

        controller.start(LessonProgress.start(lessonOnStrings(1)))
        controller.end()

        assertEquals(1, errors.size)
        assertEquals(listOf(InstrumentCommand.EndLesson), repository.sentCommands)
    }
}