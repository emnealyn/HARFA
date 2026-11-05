package com.example.harpapp.domain.model

/**
 * Stopień gamy w notacji anglosaskiej (docs/note-mapping.md).
 *
 * @property semitone odległość od C w półtonach.
 */
enum class NoteName(val semitone: Int) {
    C(0), D(2), E(4), F(5), G(7), A(9), B(11);

    /** Indeks przełącznika `lever[i]` (kolejność C, D, E, F, G, A, B – note-mapping §3). */
    val step: Int get() = ordinal

    companion object {
        fun fromChar(c: Char): NoteName? {
            val upper = c.uppercaseChar()
            return entries.firstOrNull { it.name[0] == upper }
        }

        fun fromStep(step: Int): NoteName? = entries.getOrNull(step)
    }
}
