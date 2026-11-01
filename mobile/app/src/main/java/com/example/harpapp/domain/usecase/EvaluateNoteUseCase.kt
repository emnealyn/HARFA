package com.example.harpapp.domain.usecase

class EvaluateNoteUseCase {
    operator fun invoke(expectedNote: String, playedNote: String): Boolean {
        return expectedNote.trim().uppercase() == playedNote.trim().uppercase()
    }
}
