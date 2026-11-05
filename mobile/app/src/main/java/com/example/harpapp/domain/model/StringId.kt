package com.example.harpapp.domain.model

/**
 * Indeks fizycznej struny `0..14` (docs/note-mapping.md §2).
 *
 * Nie niesie informacji o wysokości dźwięku – tę wyznacza [InstrumentState.effectiveMidi].
 * Wartość spoza zakresu nie może powstać: [of] zwraca wtedy `null`.
 */
@JvmInline
value class StringId private constructor(val index: Int) {

    override fun toString(): String = "StringId($index)"

    companion object {
        const val COUNT = 15

        val ALL: List<StringId> = List(COUNT) { StringId(it) }

        fun of(index: Int): StringId? = ALL.getOrNull(index)
    }
}
