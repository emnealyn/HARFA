package com.example.harpapp.domain.model

/** Jeden krok lekcji (docs/domain-model.md §5.1). */
data class LessonStep(
    val index: Int,
    val syllable: String,
    val pitch: Pitch
) {
    /** Trafienie, gdy harfa zagrała dźwięk o tym samym numerze MIDI. */
    fun matches(event: NoteEvent): Boolean = event.midi == pitch.midi
}

/**
 * Zweryfikowana lekcja utworzona z utworu z katalogu. Tworzy ją [LessonValidator],
 * a konstruktor pilnuje niezmienników: co najmniej jeden krok, kolejne indeksy, nuty w zakresie harfy.
 */
data class Lesson(
    val songId: Int,
    val title: String,
    val steps: List<LessonStep>
) {
    init {
        require(steps.isNotEmpty()) { "Lekcja musi mieć co najmniej jeden krok" }
        require(steps.withIndex().all { (i, step) -> step.index == i }) { "Indeksy kroków muszą być kolejne od 0" }
        require(steps.all { HarpLayout.isPlayable(it.pitch) }) { "Wszystkie nuty muszą być w zakresie harfy" }
    }
}
