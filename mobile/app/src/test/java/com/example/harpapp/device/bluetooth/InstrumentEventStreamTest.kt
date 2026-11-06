package com.example.harpapp.device.bluetooth

import com.example.harpapp.domain.model.Accidental
import com.example.harpapp.domain.model.InstrumentEvent
import com.example.harpapp.domain.model.InstrumentState
import com.example.harpapp.domain.model.NoteEvent
import com.example.harpapp.domain.model.NoteName
import com.example.harpapp.domain.model.StringId
import com.example.harpapp.protocol.DecodeResult
import com.example.harpapp.protocol.HarpCodec
import com.example.harpapp.protocol.HarpMessage
import com.example.harpapp.protocol.TestVectors
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InstrumentEventStreamTest {

    private val stream = InstrumentEventStream()

    private fun message(vectorName: String): HarpMessage {
        val frame = TestVectors.bytes(TestVectors.all.single { it.get("name").asString == vectorName })
        return (HarpCodec.decode(frame) as DecodeResult.Success).message
    }

    private fun collectNoteEvents(block: () -> Unit): List<NoteEvent> = runBlocking {
        val events = mutableListOf<NoteEvent>()
        val job = launch(start = CoroutineStart.UNDISPATCHED) { stream.noteEvents.collect { events += it } }
        block()
        yield() // pozwala kolektorowi odebrać zdarzenia z bufora przed anulowaniem
        job.cancel()
        events
    }

    @Test
    fun stringEventEmitujeIZwracaNoteEvent() {
        val expected = NoteEvent(StringId.of(11)!!, midi = 55, timestampMs = 5000)

        val events = collectNoteEvents {
            assertEquals(expected, stream.accept(message("string_event_g3")))
        }

        assertEquals(listOf(expected), events)
    }

    @Test
    fun configStateZmieniaStrojenieIZwracaConfigurationUpdated() {
        val event = stream.accept(message("config_state_a_sharp"))

        assertTrue(event is InstrumentEvent.ConfigurationUpdated)
        assertEquals(Accidental.SHARP, stream.instrumentState.value.lever(NoteName.A))
        assertEquals(stream.instrumentState.value, (event as InstrumentEvent.ConfigurationUpdated).state)
    }

    @Test
    fun resetPrzywracaStrojenieDomyslne() {
        stream.accept(message("config_state_a_sharp"))
        stream.reset()

        assertEquals(InstrumentState.DEFAULT, stream.instrumentState.value)
    }

    @Test
    fun inneINiepoprawneWiadomosciSaPomijane() {
        val events = collectNoteEvents {
            assertNull(stream.accept(message("battery_state_normal")))
            assertNull(stream.accept(HarpMessage.StringEvent(seq = 0, stringId = 15, midi = 60, tMs = 0)))
            assertNull(
                stream.accept(HarpMessage.ConfigState(seq = 0, octaveShift = 5, lever = List(7) { 0 }, baseOctave = 2))
            )
        }

        assertTrue(events.isEmpty())
        assertEquals(InstrumentState.DEFAULT, stream.instrumentState.value)
    }
}
