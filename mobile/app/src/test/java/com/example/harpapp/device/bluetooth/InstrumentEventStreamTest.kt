package com.example.harpapp.device.bluetooth

import com.example.harpapp.domain.model.Accidental
import com.example.harpapp.domain.model.BatteryPowerState
import com.example.harpapp.domain.model.BatteryStatus
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
import org.junit.Assert.assertFalse
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
    fun configStateUstawiaKonfiguracjeJakoZnana() {
        assertFalse(stream.configurationKnown.value)

        stream.accept(message("config_state_a_sharp"))

        assertTrue(stream.configurationKnown.value)
    }

    @Test
    fun resetUstawiaKonfiguracjeJakoNieznana() {
        stream.accept(message("config_state_a_sharp"))
        stream.reset()

        assertFalse(stream.configurationKnown.value)
    }

    @Test
    fun batteryStateZapisujeStanIZwracaBatteryUpdated() {
        val event = stream.accept(message("battery_state_low"))

        val expected = BatteryStatus(15, 3450, BatteryPowerState.DISCHARGING, isLow = true)
        assertEquals(InstrumentEvent.BatteryUpdated(expected), event)
        assertEquals(expected, stream.batteryStatus.value)
    }

    @Test
    fun resetCzysciStanBaterii() {
        stream.accept(message("battery_state_normal"))
        stream.reset()

        assertNull(stream.batteryStatus.value)
    }

    @Test
    fun niepoprawnyBatteryStateNieNadpisujeStanu() {
        stream.accept(message("battery_state_normal"))
        val before = stream.batteryStatus.value

        assertNull(stream.accept(HarpMessage.BatteryState(seq = 0, sid = 1, percent = 101, millivolts = 0, flags = 0)))
        assertEquals(before, stream.batteryStatus.value)
    }

    @Test
    fun inneINiepoprawneWiadomosciSaPomijane() {
        val events = collectNoteEvents {
            assertNull(stream.accept(HarpMessage.LessonEnd(seq = 0, sid = 1)))
            assertNull(stream.accept(HarpMessage.StringEvent(seq = 0, sid = 1, stringId = 15, midi = 60, tMs = 0)))
            assertNull(
                stream.accept(HarpMessage.ConfigState(seq = 0, sid = 1, octaveShift = 5, lever = List(7) { 0 }, baseOctave = 2))
            )
        }

        assertTrue(events.isEmpty())
        assertEquals(InstrumentState.DEFAULT, stream.instrumentState.value)
    }
}
