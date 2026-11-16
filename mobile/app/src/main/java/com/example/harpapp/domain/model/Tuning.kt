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
    val levers: Map<NoteName, Accidental>
) {
    fun toInstrumentState(base: InstrumentState = InstrumentState.DEFAULT): InstrumentState =
        levers.entries.fold(base) { state, (name, position) ->
            state.withLever(name, position)
        }

    fun matches(state: InstrumentState): Boolean =
        NoteName.entries.all { (levers[it] ?: Accidental.NATURAL) == state.lever(it) }
}
