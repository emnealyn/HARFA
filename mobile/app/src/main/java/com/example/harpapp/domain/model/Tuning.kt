package com.example.harpapp.domain.model

enum class TuningType {
    MAJOR,
    MINOR,
    MODAL
}

data class Tuning(
    val id: String,
    val name: String,
    val type: TuningType,
    val levers: Map<NoteName, Accidental>,
    val octaveShift: OctaveShift
) {
    fun toInstrumentState(base: InstrumentState = InstrumentState.DEFAULT): InstrumentState =
        levers.entries.fold(base.copy(octaveShift = octaveShift)) {
            state, (name, position) -> state.withLever(name, position)
        }
}



