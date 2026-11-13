package com.example.harpapp.domain.usecase

import com.example.harpapp.device.simulator.FakeInstrumentRepository
import com.example.harpapp.device.simulator.SimulatorConfig
import com.example.harpapp.domain.model.Accidental
import com.example.harpapp.domain.model.HarpLayout
import com.example.harpapp.domain.model.InstrumentCommand
import com.example.harpapp.domain.model.InstrumentState
import com.example.harpapp.domain.model.InstrumentCommand.LessonResult
import com.example.harpapp.domain.model.Lesson
import com.example.harpapp.domain.model.LessonProgress
import com.example.harpapp.domain.model.LessonStep
import com.example.harpapp.domain.model.NoteEvent
import com.example.harpapp.domain.model.StringId
import com.example.harpapp.protocol.Capabilities
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
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
        songId = "1",
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

    private fun sharpenedString(string: Int) =
        InstrumentState.DEFAULT.withLever(HarpLayout.noteNameOf(StringId.of(string)!!), Accidental.SHARP)

    private fun expectedMask(state: InstrumentState, string: Int) =
        state.stringFor(HarpLayout.basePitch(StringId.of(string)!!))?.let { 1 shl it.index } ?: 0

    @Test
    fun tuningChangeDuringStepResendsSameStepWithNewMask() = runBlocking {
        repository.reconnect()
        controller.start(LessonProgress.start(lessonOnStrings(11, 3)))
        repository.clearRecordedCommands()

        val retuned = sharpenedString(11)
        repository.emitConfiguration(retuned)

        val newMask = expectedMask(retuned, 11)
        assertTrue(newMask != 1 shl 11)
        assertEquals(
            listOf(InstrumentCommand.LessonStep(index = 0, mask = newMask)),
            repository.sentCommands
        )
        assertEquals(newMask, repository.litStrings.value)
    }

    @Test
    fun tuningChangeAfterCompleteSendsNothing() = runBlocking {
        repository.reconnect()
        val progress = LessonProgress.start(lessonOnStrings(2))
        controller.start(progress)
        controller.onUpdate(progress.onNoteEvent(noteOn(2)))
        repository.clearRecordedCommands()

        repository.emitConfiguration(sharpenedString(2))

        assertTrue(repository.sentCommands.isEmpty())
    }

    @Test
    fun tuningChangeAfterConnectionLossSendsNothingUntilResumed() = runBlocking {
        repository.reconnect()
        val progress = LessonProgress.start(lessonOnStrings(11, 3))
        controller.start(progress)

        repository.simulateConnectionLoss()
        repository.reconnect()
        repository.clearRecordedCommands()
        repository.emitConfiguration(sharpenedString(11))

        assertTrue(repository.sentCommands.isEmpty())
    }

    @Test
    fun resumeAfterReconnectUsesTuningReportedByHarp() = runBlocking {
        repository.reconnect()
        val progress = LessonProgress.start(lessonOnStrings(11, 3))
        controller.start(progress)
        repository.simulateConnectionLoss()

        repository.reconnect()
        controller.resume(progress)
        repository.clearRecordedCommands()
        val retuned = sharpenedString(11)
        repository.emitConfiguration(retuned)

        assertEquals(
            listOf(InstrumentCommand.LessonStep(index = 0, mask = expectedMask(retuned, 11))),
            repository.sentCommands
        )
    }

    private fun runLessonOn(harp: FakeInstrumentRepository) = runBlocking {
        val harpController = InstrumentLessonController.Factory(harp).create(scope) { errors += it }
        harp.reconnect()
        var progress = LessonProgress.start(lessonOnStrings(2, 4))
        harpController.start(progress)
        progress.onNoteEvent(noteOn(9)).also { harpController.onUpdate(it); progress = it.progress }
        progress.onNoteEvent(noteOn(2)).also { harpController.onUpdate(it); progress = it.progress }
        progress.onNoteEvent(noteOn(4)).also { harpController.onUpdate(it); progress = it.progress }
        harpController.end()
    }

    @Test
    fun ledsOnlyHarpGetsLedSetInsteadOfLessonCommands() {
        val harp = FakeInstrumentRepository(SimulatorConfig(capabilities = Capabilities.LEDS))

        runLessonOn(harp)

        assertEquals(
            listOf(
                InstrumentCommand.SetLeds(1 shl 2, InstrumentCommand.SetLeds.STEADY),
                InstrumentCommand.SetLeds(1 shl 4, InstrumentCommand.SetLeds.STEADY),
                InstrumentCommand.SetLeds(0, InstrumentCommand.SetLeds.STEADY),
                InstrumentCommand.SetLeds(0, InstrumentCommand.SetLeds.STEADY)
            ),
            harp.sentCommands
        )
        assertTrue(errors.isEmpty())
    }

    @Test
    fun ledsOnlyHarpGetsLedSetAfterTuningChange() = runBlocking {
        val harp = FakeInstrumentRepository(SimulatorConfig(capabilities = Capabilities.LEDS))
        val harpController = InstrumentLessonController.Factory(harp).create(scope) { errors += it }
        harp.reconnect()
        harpController.start(LessonProgress.start(lessonOnStrings(11, 3)))
        harp.clearRecordedCommands()

        val retuned = sharpenedString(11)
        harp.emitConfiguration(retuned)

        assertEquals(
            listOf(InstrumentCommand.SetLeds(expectedMask(retuned, 11), InstrumentCommand.SetLeds.STEADY)),
            harp.sentCommands
        )
    }

    @Test
    fun harpWithoutLedsAndLessonReceivesNoLessonCommands() {
        val harp = FakeInstrumentRepository(SimulatorConfig(capabilities = Capabilities.BATTERY))

        runLessonOn(harp)

        assertTrue(harp.sentCommands.isEmpty())
        assertTrue(errors.isEmpty())
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

    @Test
    fun sendCancelledByClosedConnectionDoesNotStopControllerNorReportError() = runBlocking {
        repository.reconnect()
        repository.failNextCommand(CancellationException("BLE GATT connection was cancelled"))
        val progress = LessonProgress.start(lessonOnStrings(11, 3))

        controller.start(progress)
        controller.resume(progress)

        assertTrue(errors.isEmpty())
        assertEquals(
            listOf(
                InstrumentCommand.StartLesson(totalSteps = 2),
                InstrumentCommand.LessonStep(index = 0, mask = 1 shl 11)
            ),
            repository.sentCommands
        )
    }

    @Test
    fun tuningChangeAfterCancelledSendSendsNothing() = runBlocking {
        repository.reconnect()
        repository.failNextCommand(CancellationException("BLE GATT connection was cancelled"))
        controller.start(LessonProgress.start(lessonOnStrings(11, 3)))

        repository.emitConfiguration(sharpenedString(11))

        assertTrue(repository.sentCommands.isEmpty())
        assertTrue(errors.isEmpty())
    }

    @Test
    fun signalQueuedBeforeReconnectIsNotSentInNewSession() = runTest {
        // Kolejka sygnałów obsługiwana dopiero przez runCurrent(), jak przy zajętym wątku kontrolera
        val queuedController = InstrumentLessonController.Factory(repository).create(backgroundScope) { errors += it }
        repository.reconnect()
        var progress = LessonProgress.start(lessonOnStrings(2, 4))
        queuedController.start(progress)
        runCurrent()
        repository.clearRecordedCommands()

        // Trafienie zakolejkowane w sesji 1, obsłużone dopiero po ponownym połączeniu (CR HARFA-14)
        progress.onNoteEvent(noteOn(2)).also { queuedController.onUpdate(it); progress = it.progress }
        queuedController.end()
        repository.simulateConnectionLoss()
        repository.reconnect()
        runCurrent()

        assertTrue(repository.sentCommands.isEmpty())
        assertEquals(0, repository.litStrings.value)
        assertTrue(errors.isEmpty())

        // Wznowienie w nowej sesji zaczyna lekcję od nowa od bieżącego kroku
        queuedController.resume(progress)
        runCurrent()
        assertEquals(
            listOf(
                InstrumentCommand.StartLesson(totalSteps = 2),
                InstrumentCommand.LessonStep(index = 1, mask = 1 shl 4)
            ),
            repository.sentCommands
        )
    }

    @Test
    fun pauseSendsLessonPauseInsteadOfEndingLesson() = runBlocking {
        repository.reconnect()
        controller.start(LessonProgress.start(lessonOnStrings(11, 3)))
        repository.clearRecordedCommands()

        controller.pause()

        assertEquals(listOf(InstrumentCommand.PauseLesson(paused = true)), repository.sentCommands)
        assertEquals(0, repository.litStrings.value)
    }

    @Test
    fun resumeInSameSessionSendsOnlyLessonPauseOff() = runBlocking {
        repository.reconnect()
        val progress = LessonProgress.start(lessonOnStrings(11, 3))
        controller.start(progress)
        controller.pause()
        repository.clearRecordedCommands()

        controller.resume(progress)

        assertEquals(listOf(InstrumentCommand.PauseLesson(paused = false)), repository.sentCommands)
        assertEquals(1 shl 11, repository.litStrings.value)
    }

    @Test
    fun resumeAfterReconnectDuringPauseStartsLessonAgain() = runBlocking {
        repository.reconnect()
        val progress = LessonProgress.start(lessonOnStrings(11, 3))
        controller.start(progress)
        controller.pause()

        repository.simulateConnectionLoss()
        repository.reconnect()
        repository.clearRecordedCommands()
        controller.resume(progress)

        assertEquals(
            listOf(
                InstrumentCommand.StartLesson(totalSteps = 2),
                InstrumentCommand.LessonStep(index = 0, mask = 1 shl 11)
            ),
            repository.sentCommands
        )
    }

    @Test
    fun tuningChangeDuringPauseIsSentAfterResume() = runBlocking {
        repository.reconnect()
        val progress = LessonProgress.start(lessonOnStrings(11, 3))
        controller.start(progress)
        controller.pause()
        repository.clearRecordedCommands()

        val retuned = sharpenedString(11)
        repository.emitConfiguration(retuned)
        assertTrue(repository.sentCommands.isEmpty())

        controller.resume(progress)

        assertEquals(
            listOf(
                InstrumentCommand.PauseLesson(paused = false),
                InstrumentCommand.LessonStep(index = 0, mask = expectedMask(retuned, 11))
            ),
            repository.sentCommands
        )
    }

    @Test
    fun ledsOnlyHarpPauseTurnsLedsOffAndResumeRestoresThem() = runBlocking {
        val harp = FakeInstrumentRepository(SimulatorConfig(capabilities = Capabilities.LEDS))
        val harpController = InstrumentLessonController.Factory(harp).create(scope) { errors += it }
        harp.reconnect()
        val progress = LessonProgress.start(lessonOnStrings(11, 3))
        harpController.start(progress)
        harp.clearRecordedCommands()

        harpController.pause()
        harpController.resume(progress)

        assertEquals(
            listOf(
                InstrumentCommand.SetLeds(0, InstrumentCommand.SetLeds.STEADY),
                InstrumentCommand.SetLeds(1 shl 11, InstrumentCommand.SetLeds.STEADY)
            ),
            harp.sentCommands
        )
        assertTrue(errors.isEmpty())
    }
}
