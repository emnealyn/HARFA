package com.example.harpapp.domain.usecase

import com.example.harpapp.domain.model.CatalogStep
import com.example.harpapp.domain.model.LessonValidation
import com.example.harpapp.domain.model.LessonValidator
import com.example.harpapp.domain.model.Song

/**
 * Przygotowuje lekcję z utworu z katalogu: przekształca surowe nuty utworu na kroki
 * i waliduje je przed rozpoczęciem nauki (docs/domain-model.md §6). Nigdy nie zgłasza wyjątku.
 */
class PrepareLessonUseCase {
    operator fun invoke(song: Song): LessonValidation =
        LessonValidator.validate(
            songId = song.id,
            title = song.title,
            steps = song.lyricsWithNotes.map { CatalogStep(syllable = it.syllable, note = it.note) }
        )
}
