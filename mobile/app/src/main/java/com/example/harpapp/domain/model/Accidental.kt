package com.example.harpapp.domain.model

/**
 * Znak chromatyczny. Ten sam typ opisuje stan przełącznika (`LeverOffset ∈ {-1, 0, +1}`,
 * docs/note-mapping.md §3).
 */
enum class Accidental(val offset: Int, val symbol: String) {
    FLAT(-1, "b"),
    NATURAL(0, ""),
    SHARP(+1, "#");

    companion object {
        fun fromOffset(offset: Int): Accidental? = entries.firstOrNull { it.offset == offset }
    }
}
