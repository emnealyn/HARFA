package com.example.harpapp.device.simulator

import com.example.harpapp.domain.model.InstrumentDevice
import com.example.harpapp.protocol.Capabilities

/** Parametry symulatora. Bez losowości, więc ten sam scenariusz zawsze daje ten sam wynik. */
data class SimulatorConfig(
    val deviceName: String = "HARFA-SIM1",
    val deviceAddress: String = "SIMULATOR",
    val deviceId: Long = 0x0000_5131L,
    val capabilities: Int = Capabilities.LEDS or Capabilities.BATTERY or Capabilities.LESSON,
    val scanDelayMs: Long = 0,
    val connectDelayMs: Long = 0,
    val batteryPercent: Int = 80,
    val batteryMillivolts: Int = 3900
) {
    val device: InstrumentDevice get() = InstrumentDevice(deviceName, deviceAddress)

    companion object {
        /** Konfiguracja trybu demo: krótkie opóźnienia, żeby było widać stan CONNECTING. */
        val DEMO = SimulatorConfig(scanDelayMs = 800, connectDelayMs = 600)
    }
}