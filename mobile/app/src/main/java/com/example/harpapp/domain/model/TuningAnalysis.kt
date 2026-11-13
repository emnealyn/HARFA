package com.example.harpapp.domain.model

data class LeverChange(
    val name: NoteName,
    val from: Accidental,
    val to: Accidental
)

data class NoteConflict(
    val name: NoteName,
    val pitches: List<Pitch>
)

sealed interface TuningAnalysis {
    data object Compatible : TuningAnalysis

    data object Incompatible : TuningAnalysis

    data class NeedsChanges(
        val levers: List<LeverChange>,
        val octave: OctaveShift?,
        val target: InstrumentState
    ) : TuningAnalysis

    data class Conflict(val conflicts: List<NoteConflict>) : TuningAnalysis

    data class OutOfRange(val pitches: List<Pitch>) : TuningAnalysis
}
