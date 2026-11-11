package com.example.harpapp.domain.model

/**
 * Aktualne strojenie instrumentu (docs/domain-model.md §3). Źródłem prawdy jest harfa
 * (ramka `CONFIG_STATE`), aplikacja jedynie je odtwarza.
 */
data class InstrumentState(
    val levers: Map<NoteName, Accidental> = NoteName.entries.associateWith { Accidental.NATURAL },
    val octaveShift: OctaveShift = OctaveShift.NONE,
    val baseOctave: Int = HarpLayout.BASE_OCTAVE
) {
    fun lever(name: NoteName): Accidental = levers[name] ?: Accidental.NATURAL

    fun withLever(name: NoteName, position: Accidental): InstrumentState =
        copy(levers = levers + (name to position))

    /** Wzór z note-mapping §4: `BaseMIDI[id] + LeverOffset[Step(id)] + OctaveShift × 12`. */
    fun effectiveMidi(id: StringId): Int =
        HarpLayout.basePitch(id, baseOctave).midi +
            lever(HarpLayout.noteNameOf(id)).offset +
            octaveShift.offset * 12

    fun pitchOf(id: StringId): Pitch {
        val name = HarpLayout.noteNameOf(id)
        val octave = HarpLayout.basePitch(id, baseOctave).octave + octaveShift.offset
        return Pitch(name, lever(name), octave)
    }

    val lowestPitch: Pitch get() = pitchOf(StringId.ALL.first())

    val highestPitch: Pitch get() = pitchOf(StringId.ALL.last())

    /**
     * Struna, która przy obecnym strojeniu wyda [pitch] (porównanie po MIDI).
     * `null` – dźwięk poza zakresem albo wymaga innego ustawienia przełączników lub transpozycji.
     * Gdy kilka strun daje ten sam dźwięk (np. B#2 i C3), zwracana jest najniższa.
     */
    fun stringFor(pitch: Pitch): StringId? =
        StringId.ALL.firstOrNull { effectiveMidi(it) == pitch.midi }

    companion object {
        val DEFAULT = InstrumentState()
    }
}
