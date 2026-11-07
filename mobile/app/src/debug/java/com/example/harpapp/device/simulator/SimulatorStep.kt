package com.example.harpapp.device.simulator

/** Kroki deterministycznego scenariusza dla [FakeInstrumentRepository.runScenario]. */
sealed interface SimulatorStep {
    data object Scan : SimulatorStep
    data object Connect : SimulatorStep
    data object Disconnect : SimulatorStep
    data object LoseConnection : SimulatorStep
    data object Reconnect : SimulatorStep
    data class Pluck(val stringId: Int) : SimulatorStep
    data object PluckLit : SimulatorStep
    data class Battery(val percent: Int, val millivolts: Int = 3900, val flags: Int = 0) : SimulatorStep
    data class Error(val code: Int, val referenceSequence: Int = 0xFF, val referenceType: Int = 0) : SimulatorStep
    data class Wait(val millis: Long) : SimulatorStep
}