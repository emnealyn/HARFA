package com.example.harpapp.domain.model

/** Transpozycja oktawowa instrumentu (docs/note-mapping.md §3). */
enum class OctaveShift(val offset: Int) {
    DOWN(-1),
    NONE(0),
    UP(+1);

    companion object {
        fun fromOffset(offset: Int): OctaveShift? = entries.firstOrNull { it.offset == offset }
    }
}
