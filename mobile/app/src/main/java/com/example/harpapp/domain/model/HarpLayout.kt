package com.example.harpapp.domain.model

/** Stałe układu strun harfy (docs/note-mapping.md §2 i §5). */
object HarpLayout {
    /** Oktawa najniższej struny w obecnej konstrukcji (C2). */
    const val BASE_OCTAVE = 2

    /** Najniższy osiągalny dźwięk: B0 (C2, przełącznik −1, transpozycja −1). */
    const val MIN_MIDI = 23

    /** Najwyższy osiągalny dźwięk: C#5 (C4, przełącznik +1, transpozycja +1). */
    const val MAX_MIDI = 73

    /** Stopień gamy struny: 0 → C, 1 → D, …, 6 → B, 7 → C, … */
    fun noteNameOf(id: StringId): NoteName = NoteName.entries[id.index % NoteName.entries.size]

    fun stringsOf(name: NoteName): List<StringId> = StringId.ALL.filter { noteNameOf(it) == name }

    /** Dźwięk struny przy przełącznikach naturalnych i bez transpozycji. */
    fun basePitch(id: StringId, baseOctave: Int = BASE_OCTAVE): Pitch =
        Pitch(noteNameOf(id), Accidental.NATURAL, baseOctave + id.index / NoteName.entries.size)

    /** Czy dźwięk da się zagrać przy jakimkolwiek ustawieniu instrumentu. */
    fun isPlayable(pitch: Pitch): Boolean = pitch.midi in MIN_MIDI..MAX_MIDI
}
