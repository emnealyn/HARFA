package com.example.harpapp.device.bluetooth

import com.example.harpapp.domain.model.Accidental
import com.example.harpapp.domain.model.InstrumentCapabilities
import com.example.harpapp.domain.model.InstrumentState
import com.example.harpapp.domain.model.NoteEvent
import com.example.harpapp.domain.model.NoteName
import com.example.harpapp.domain.model.OctaveShift
import com.example.harpapp.domain.model.StringId
import com.example.harpapp.protocol.Capabilities
import com.example.harpapp.protocol.HarpMessage
import com.example.harpapp.domain.model.BatteryPowerState
import com.example.harpapp.domain.model.BatteryStatus
import com.example.harpapp.protocol.BatteryFlags
import com.example.harpapp.protocol.BatteryPercent

// Odwzorowanie ramek protokołu na model domenowy (docs/domain-model.md §7).
// Niepoprawne wartości dają null zamiast wyjątku.

fun HarpMessage.StringEvent.toNoteEvent(): NoteEvent? {
    val id = StringId.of(stringId) ?: return null
    return NoteEvent(stringId = id, midi = midi, timestampMs = tMs)
}

fun instrumentCapabilitiesOf(caps: Int): InstrumentCapabilities = InstrumentCapabilities(
    leds = caps and Capabilities.LEDS != 0,
    battery = caps and Capabilities.BATTERY != 0,
    lesson = caps and Capabilities.LESSON != 0
)

fun HarpMessage.ConfigState.toInstrumentState(): InstrumentState? {
    if (lever.size != NoteName.entries.size) return null
    val shift = OctaveShift.fromOffset(octaveShift) ?: return null

    val levers = NoteName.entries.associateWith { name ->
        Accidental.fromOffset(lever[name.step]) ?: return null
    }
    return InstrumentState(levers = levers, octaveShift = shift, baseOctave = baseOctave)
}

fun HarpMessage.BatteryState.toBatteryStatus(): BatteryStatus? {
    val knownPercent = when (percent) {
        BatteryPercent.UNKNOWN -> null
        in 0..100 -> percent
        else -> return null
    }
    val powerState = when {
        flags and BatteryFlags.CHARGING != 0 -> BatteryPowerState.CHARGING
        flags and BatteryFlags.EXTERNAL_POWER != 0 -> BatteryPowerState.EXTERNAL_POWER
        else -> BatteryPowerState.DISCHARGING
    }
    return BatteryStatus(
        percent = knownPercent,
        millivolts = millivolts.takeIf { it != 0 },
        powerState = powerState,
        isLow = flags and BatteryFlags.LOW != 0
    )
}
