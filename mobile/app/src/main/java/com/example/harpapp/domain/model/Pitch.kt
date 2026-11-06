package com.example.harpapp.domain.model

/**
 * Wysokość dźwięku: nazwa nuty, znak chromatyczny i oktawa (docs/domain-model.md §2.4).
 *
 * Równość porównuje pisownię (`G4 != G#4 != G5`, `G#4 != Ab4`).
 * Dźwięki o tym samym [midi] rozpoznaje [isEnharmonicWith].
 */
data class Pitch(
    val name: NoteName,
    val accidental: Accidental,
    val octave: Int
) {
    /** Numer MIDI, zgodny z tabelami note-mapping (C2 = 36, C4 = 60). */
    val midi: Int get() = (octave + 1) * 12 + name.semitone + accidental.offset

    fun isEnharmonicWith(other: Pitch): Boolean = midi == other.midi

    override fun toString(): String = "${name.name}${accidental.symbol}$octave"

    companion object {
        const val MIN_OCTAVE = 0
        const val MAX_OCTAVE = 8

        private val SHARP_SPELLING: List<Pair<NoteName, Accidental>> = listOf(
            NoteName.C to Accidental.NATURAL,
            NoteName.C to Accidental.SHARP,
            NoteName.D to Accidental.NATURAL,
            NoteName.D to Accidental.SHARP,
            NoteName.E to Accidental.NATURAL,
            NoteName.F to Accidental.NATURAL,
            NoteName.F to Accidental.SHARP,
            NoteName.G to Accidental.NATURAL,
            NoteName.G to Accidental.SHARP,
            NoteName.A to Accidental.NATURAL,
            NoteName.A to Accidental.SHARP,
            NoteName.B to Accidental.NATURAL
        )

        /** Dźwięk o danym numerze MIDI, zapisany z krzyżykami (jak pliki `cs`, `ds`… w note-mapping §5). */
        fun fromMidi(midi: Int): Pitch {
            val (name, accidental) = SHARP_SPELLING[Math.floorMod(midi, 12)]
            return Pitch(name, accidental, Math.floorDiv(midi, 12) - 1)
        }

        /**
         * Parsuje zapis `<litera A–G><# | b>?<oktawa 0–8>`, np. `G4`, `G#4`, `Bb3`.
         * Białe znaki są obcinane, wielkość litery nuty nie ma znaczenia. Nigdy nie zgłasza wyjątku.
         */
        fun parse(raw: String?): PitchParseResult {
            val text = raw?.trim().orEmpty()
            if (text.isEmpty()) return PitchParseResult.Invalid(PitchError.EMPTY)

            val malformed = PitchParseResult.Invalid(PitchError.MALFORMED)
            val name = NoteName.fromChar(text[0]) ?: return malformed

            val accidental = when (text.getOrNull(1)) {
                '#' -> Accidental.SHARP
                'b' -> Accidental.FLAT
                else -> Accidental.NATURAL
            }
            val octaveText = text.substring(if (accidental == Accidental.NATURAL) 1 else 2)
            if (octaveText.length != 1) return malformed

            val octave = octaveText[0] - '0'
            if (octave !in MIN_OCTAVE..MAX_OCTAVE) return malformed

            return PitchParseResult.Valid(Pitch(name, accidental, octave))
        }
    }
}

enum class PitchError { EMPTY, MALFORMED }

sealed interface PitchParseResult {
    data class Valid(val pitch: Pitch) : PitchParseResult
    data class Invalid(val error: PitchError) : PitchParseResult
}
