package com.example.harpapp.domain.model

/**
 * Zagranie struny (domenowa postać `STRING_EVENT`, docs/domain-model.md §4).
 *
 * @property midi efektywny numer MIDI podany przez harfę – wartość autorytatywna,
 * aplikacja jej nie przelicza.
 * @property timestampMs znacznik czasu harfy (mod 65 536), tylko do liczenia odstępów.
 */
data class NoteEvent(
    val stringId: StringId,
    val midi: Int,
    val timestampMs: Int
) : InstrumentEvent {
    val pitch: Pitch get() = Pitch.fromMidi(midi)
}
