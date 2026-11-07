package com.example.harpapp.di

import android.content.Context
import com.example.harpapp.device.bluetooth.BluetoothInstrumentRepository
import com.example.harpapp.device.simulator.FakeInstrumentRepository
import com.example.harpapp.device.simulator.SimulatorConfig
import com.example.harpapp.presentation.dev.DebugDevTools
import com.example.harpapp.presentation.dev.SimulatorPreferences

/** Debug: symulator albo prawdziwa harfa, zależnie od przełącznika w Ustawieniach. */
object InstrumentModule {
    fun create(context: Context): InstrumentSetup {
        val prefs = SimulatorPreferences(context)
        val simulator = if (prefs.isSimulatorEnabled) FakeInstrumentRepository(SimulatorConfig.DEMO) else null
        return InstrumentSetup(
            repository = simulator ?: BluetoothInstrumentRepository(context),
            devTools = DebugDevTools(prefs, simulator)
        )
    }
}