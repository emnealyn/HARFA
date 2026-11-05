package com.example.harpapp.domain.usecase

import java.util.Locale

class EvaluateNoteUseCase {
    operator fun invoke(expectedNote: String, playedNote: String): Boolean {
        return expectedNote.trim().uppercase(Locale.ROOT) ==
            playedNote.trim().uppercase(Locale.ROOT)
    }
}
