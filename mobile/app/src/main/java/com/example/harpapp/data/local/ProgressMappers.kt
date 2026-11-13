package com.example.harpapp.data.local

import com.example.harpapp.domain.model.Lesson
import com.example.harpapp.domain.model.LessonProgress
import com.example.harpapp.domain.model.LessonStatus

fun SongProgressEntity.toDomain(lesson: Lesson): LessonProgress {
    return LessonProgress(
        lesson = lesson,
        currentIndex = currentIndex,
        status = LessonStatus.valueOf(status),
        hits = hits,
        misses = misses
    )
}

fun LessonProgress.toEntity(): SongProgressEntity {
    return SongProgressEntity(
        songId = lesson.songId,
        currentIndex = currentIndex,
        hits = hits,
        misses = misses,
        status = status.name,
        updatedAt = System.currentTimeMillis()
    )
}
