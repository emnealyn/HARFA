package com.example.harpapp.domain.model

/**
 * Surowy krok z katalogu utworów (np. `songs.json`). Pola są nullable, bo dane pochodzą
 * z deserializacji i mogą być niekompletne.
 */
data class CatalogStep(
    val syllable: String?,
    val note: String?
)

enum class CatalogIssueReason { NO_STEPS, EMPTY, MALFORMED, OUT_OF_RANGE }

/**
 * Problem w danych utworu. [stepIndex] i [raw] są `null` dla problemów dotyczących całego utworu
 * ([CatalogIssueReason.NO_STEPS]).
 */
data class CatalogIssue(
    val stepIndex: Int?,
    val raw: String?,
    val reason: CatalogIssueReason
)

sealed interface LessonValidation {
    data class Valid(val lesson: Lesson) : LessonValidation
    data class Invalid(val issues: List<CatalogIssue>) : LessonValidation
}

/** Walidacja katalogu przed rozpoczęciem lekcji (docs/domain-model.md §6). Nigdy nie zgłasza wyjątku. */
object LessonValidator {

    fun validate(songId: String, title: String?, steps: List<CatalogStep?>?): LessonValidation {
        if (steps.isNullOrEmpty()) {
            return LessonValidation.Invalid(listOf(CatalogIssue(null, null, CatalogIssueReason.NO_STEPS)))
        }

        val issues = mutableListOf<CatalogIssue>()
        val lessonSteps = mutableListOf<LessonStep>()

        steps.forEachIndexed { index, step ->
            val raw = step?.note
            when (val parsed = Pitch.parse(raw)) {
                is PitchParseResult.Invalid ->
                    issues += CatalogIssue(index, raw, parsed.error.toIssueReason())

                is PitchParseResult.Valid ->
                    if (HarpLayout.isPlayable(parsed.pitch)) {
                        lessonSteps += LessonStep(index, step?.syllable.orEmpty(), parsed.pitch)
                    } else {
                        issues += CatalogIssue(index, raw, CatalogIssueReason.OUT_OF_RANGE)
                    }
            }
        }

        return if (issues.isEmpty()) {
            LessonValidation.Valid(Lesson(songId, title.orEmpty(), lessonSteps))
        } else {
            LessonValidation.Invalid(issues)
        }
    }

    private fun PitchError.toIssueReason(): CatalogIssueReason = when (this) {
        PitchError.EMPTY -> CatalogIssueReason.EMPTY
        PitchError.MALFORMED -> CatalogIssueReason.MALFORMED
    }
}
