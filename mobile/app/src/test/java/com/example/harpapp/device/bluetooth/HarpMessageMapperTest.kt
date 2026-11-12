package com.example.harpapp.device.bluetooth

import com.example.harpapp.domain.model.Accidental
import com.example.harpapp.domain.model.InstrumentCapabilities
import com.example.harpapp.domain.model.BatteryPowerState
import com.example.harpapp.domain.model.BatteryStatus
import com.example.harpapp.domain.model.InstrumentState
import com.example.harpapp.domain.model.NoteEvent
import com.example.harpapp.domain.model.NoteName
import com.example.harpapp.domain.model.OctaveShift
import com.example.harpapp.domain.model.StringId
import com.example.harpapp.protocol.Capabilities
import com.example.harpapp.protocol.DecodeResult
import com.example.harpapp.protocol.HarpCodec
import com.example.harpapp.protocol.HarpMessage
import com.example.harpapp.protocol.TestVectors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HarpMessageMapperTest {

    private fun decodeVector(name: String): HarpMessage {
        val vector = TestVectors.all.single { it.get("name").asString == name }
        val result = HarpCodec.decode(TestVectors.bytes(vector))
        return (result as DecodeResult.Success).message
    }

    @Test
    fun stringEventZWektoraTestowegoDajeNoteEvent() {
        val event = (decodeVector("string_event_a_sharp2") as HarpMessage.StringEvent).toNoteEvent()

        assertEquals(NoteEvent(StringId.of(5)!!, midi = 46, timestampMs = 1250), event)
        assertEquals("A#2", event!!.pitch.toString())
    }

    @Test
    fun wszystkiePoprawneWektoryStringEventMapujaSieBezUtratyDanych() {
        val vectors = TestVectors.all.filter {
            it.get("valid").asBoolean &&
                it.getAsJsonObject("decoded")?.get("type")?.asString == "STRING_EVENT"
        }
        assertTrue(vectors.isNotEmpty())

        vectors.forEach { vector ->
            val message = (HarpCodec.decode(TestVectors.bytes(vector)) as DecodeResult.Success)
                .message as HarpMessage.StringEvent
            val event = message.toNoteEvent()!!
            assertEquals(message.stringId, event.stringId.index)
            assertEquals(message.midi, event.midi)
            assertEquals(message.tMs, event.timestampMs)
        }
    }

    @Test
    fun configStateNaturalnyDajeStanDomyslny() {
        val state = (decodeVector("config_state_natural") as HarpMessage.ConfigState).toInstrumentState()

        assertEquals(InstrumentState.DEFAULT, state)
    }

    @Test
    fun configStateZKrzyzykiemNaAPodnosiStrune5() {
        val state = (decodeVector("config_state_a_sharp") as HarpMessage.ConfigState).toInstrumentState()!!

        assertEquals(Accidental.SHARP, state.lever(NoteName.A))
        assertEquals(46, state.effectiveMidi(StringId.of(5)!!))
    }

    @Test
    fun configStateSkrajneWartosci() {
        val state = (decodeVector("config_state_extremes") as HarpMessage.ConfigState).toInstrumentState()!!

        assertEquals(OctaveShift.DOWN, state.octaveShift)
        assertEquals(
            listOf(
                Accidental.FLAT, Accidental.SHARP, Accidental.FLAT, Accidental.SHARP,
                Accidental.FLAT, Accidental.SHARP, Accidental.FLAT
            ),
            NoteName.entries.map { state.lever(it) }
        )
    }

    @Test
    fun maskaCapsZDeviceInfoDajeMozliwosciInstrumentu() {
        assertEquals(InstrumentCapabilities.NONE, instrumentCapabilitiesOf(0))
        assertEquals(
            InstrumentCapabilities(leds = true, battery = false, lesson = false),
            instrumentCapabilitiesOf(Capabilities.LEDS)
        )
        assertEquals(
            InstrumentCapabilities(leds = true, battery = true, lesson = true),
            instrumentCapabilitiesOf(Capabilities.LEDS or Capabilities.BATTERY or Capabilities.LESSON or Capabilities.DISPLAY)
        )
    }

    @Test
    fun niepoprawneWartosciDajaNullZamiastWyjatku() {
        assertNull(HarpMessage.StringEvent(seq = 0, stringId = 15, midi = 60, tMs = 0).toNoteEvent())
        assertNull(HarpMessage.ConfigState(seq = 0, octaveShift = 2, lever = List(7) { 0 }, baseOctave = 2).toInstrumentState())
        assertNull(HarpMessage.ConfigState(seq = 0, octaveShift = 0, lever = List(6) { 0 }, baseOctave = 2).toInstrumentState())
        assertNull(HarpMessage.ConfigState(seq = 0, octaveShift = 0, lever = List(7) { if (it == 3) 2 else 0 }, baseOctave = 2).toInstrumentState())
        assertNull(HarpMessage.BatteryState(seq = 0, percent = 101, millivolts = 3900, flags = 0).toBatteryStatus())
    }

    private fun batteryVector(name: String): BatteryStatus? =
        (decodeVector(name) as HarpMessage.BatteryState).toBatteryStatus()

    private fun batteryWithFlags(flags: Int): BatteryStatus =
        HarpMessage.BatteryState(seq = 0, percent = 50, millivolts = 3800, flags = flags).toBatteryStatus()!!

    @Test
    fun batteryStateBezFlagOznaczaRozladowywanie() {
        assertEquals(
            BatteryStatus(80, 3900, BatteryPowerState.DISCHARGING, isLow = false),
            batteryVector("battery_state_normal")
        )
    }

    @Test
    fun batteryStateZFlagaLowOznaczaNiskiPoziom() {
        assertEquals(
            BatteryStatus(15, 3450, BatteryPowerState.DISCHARGING, isLow = true),
            batteryVector("battery_state_low")
        )
    }

    @Test
    fun batteryStateZeWszystkimiFlagamiToLadowanieINiskiPoziom() {
        assertEquals(
            BatteryStatus(15, 3450, BatteryPowerState.CHARGING, isLow = true),
            batteryVector("battery_state_low_charging")
        )
    }

    @Test
    fun batteryStateNieznanyNieMaLiczby() {
        assertEquals(
            BatteryStatus(null, null, BatteryPowerState.DISCHARGING, isLow = false),
            batteryVector("battery_state_unknown")
        )
    }

    @Test
    fun flagiZasilaniaWybierajaJedenStan() {
        assertEquals(BatteryPowerState.CHARGING, batteryWithFlags(1).powerState)
        assertEquals(BatteryPowerState.EXTERNAL_POWER, batteryWithFlags(2).powerState)
        assertEquals(BatteryPowerState.CHARGING, batteryWithFlags(3).powerState)
    }
}
