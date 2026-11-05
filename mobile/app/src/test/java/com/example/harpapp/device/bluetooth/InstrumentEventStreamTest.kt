package com.example.harpapp.device.bluetooth

import com.example.harpapp.domain.model.Accidental
import com.example.harpapp.domain.model.InstrumentState
import com.example.harpapp.domain.model.NoteEvent
import com.example.harpapp.domain.model.NoteName
import com.example.harpapp.domain.model.StringId
import com.example.harpapp.protocol.TestVectors
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InstrumentEventStreamTest {

    private val stream = InstrumentEventStream()

    private fun frame(vectorName: String): ByteArray =
        TestVectors.bytes(TestVectors.all.single { it.get("name").asString == vectorName })

    private fun collectNoteEvents(block: () -> Unit): List<NoteEvent> = runBlocking {
        val events = mutableListOf<NoteEvent>()
        val job = launch(start = CoroutineStart.UNDISPATCHED) { stream.noteEvents.collect { events += it } }
        block()
        job.cancel()
        events
    }

    @Test
    fun ramkaStringEventEmitujeNoteEvent() {
        val events = collectNoteEvents { assertTrue(stream.accept(frame("string_event_g3"))) }

        assertEquals(listOf(NoteEvent(StringId.of(11)!!, midi = 55, timestampMs = 5000)), events)
    }

    @Test
    fun ramkaConfigStateZmieniaStrojenie() {
        assertTrue(stream.accept(frame("config_state_a_sharp")))

        assertEquals(Accidental.SHARP, stream.instrumentState.value.lever(NoteName.A))
    }

    @Test
    fun resetPrzywracaStrojenieDomyslne() {
        stream.accept(frame("config_state_a_sharp"))
        stream.reset()

        assertEquals(InstrumentState.DEFAULT, stream.instrumentState.value)
    }

    @Test
    fun niepoprawneIInneRamkiSaPomijane() {
        val events = collectNoteEvents {
            assertFalse(stream.accept(ByteArray(0)))
            assertFalse(stream.accept("F\n".toByteArray()))
            assertFalse(stream.accept(frame("battery_state_normal")))
        }

        assertTrue(events.isEmpty())
        assertEquals(InstrumentState.DEFAULT, stream.instrumentState.value)
    }
}
