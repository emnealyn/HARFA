package com.example.harpapp.domain.usecase

import java.util.Locale

class EvaluateNoteUseCase {
    operator fun invoke(expectedNote: String, playedNote: String): Boolean {
        return expectedNote.trim().uppercase(Locale.ROOT) ==
            playedNote.trim().uppercase(Locale.ROOT)
    }

    operator fun invoke(expectedNote: String, playedMidi: Int): Boolean {
        return noteToMidi(expectedNote) == playedMidi
    }

    private fun noteToMidi(note: String): Int? {
        val match = NOTE_PATTERN.matchEntire(note.trim()) ?: return null
        val base = when (match.groupValues[1].uppercase(Locale.ROOT)) {
            "C" -> 0
            "D" -> 2
            "E" -> 4
            "F" -> 5
            "G" -> 7
            "A" -> 9
            "B" -> 11
            else -> return null
        }
        val accidental = when (match.groupValues[2]) {
            "#" -> 1
            "b" -> -1
            else -> 0
        }
        val octave = match.groupValues[3].toIntOrNull() ?: return null
        return (octave + 1) * 12 + base + accidental
    }

    private companion object {
        val NOTE_PATTERN = Regex("^([A-Ga-g])([#b]?)(-?\\d+)$")
    }
}
