package com.example.harpapp.domain.model

enum class LessonStatus { IN_PROGRESS, PAUSED, COMPLETED }

enum class StepOutcome { HIT, MISS, COMPLETE, IGNORED }

data class ProgressUpdate(
    val progress: LessonProgress,
    val outcome: StepOutcome
)

/**
 * Niemutowalny stan przebiegu lekcji (docs/domain-model.md §5.2).
 * Po ukończeniu [currentIndex] jest równy liczbie kroków.
 */
data class LessonProgress(
    val lesson: Lesson,
    val currentIndex: Int = 0,
    val status: LessonStatus = LessonStatus.IN_PROGRESS,
    val hits: Int = 0,
    val misses: Int = 0
) {
    init {
        require(currentIndex in 0..lesson.steps.size) { "currentIndex poza zakresem lekcji" }
        require((status == LessonStatus.COMPLETED) == (currentIndex == lesson.steps.size)) {
            "Lekcja jest ukończona wtedy i tylko wtedy, gdy currentIndex == liczba kroków"
        }
    }

    val currentStep: LessonStep? get() = lesson.steps.getOrNull(currentIndex)

    val isCompleted: Boolean get() = status == LessonStatus.COMPLETED

    fun onNoteEvent(event: NoteEvent): ProgressUpdate {
        val step = currentStep
        if (status != LessonStatus.IN_PROGRESS || step == null) {
            return ProgressUpdate(this, StepOutcome.IGNORED)
        }
        if (!step.matches(event)) {
            return ProgressUpdate(copy(misses = misses + 1), StepOutcome.MISS)
        }
        val nextIndex = currentIndex + 1
        return if (nextIndex == lesson.steps.size) {
            ProgressUpdate(
                copy(currentIndex = nextIndex, status = LessonStatus.COMPLETED, hits = hits + 1),
                StepOutcome.COMPLETE
            )
        } else {
            ProgressUpdate(copy(currentIndex = nextIndex, hits = hits + 1), StepOutcome.HIT)
        }
    }

    /** Wstrzymanie, np. po utracie BLE (ADR 001). Zachowuje bieżący krok. */
    fun pause(): LessonProgress =
        if (status == LessonStatus.IN_PROGRESS) copy(status = LessonStatus.PAUSED) else this

    fun resume(): LessonProgress =
        if (status == LessonStatus.PAUSED) copy(status = LessonStatus.IN_PROGRESS) else this

    companion object {
        fun start(lesson: Lesson): LessonProgress = LessonProgress(lesson)
    }
}
