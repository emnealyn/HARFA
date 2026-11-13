package com.example.harpapp.data.local

import com.example.harpapp.domain.model.Lesson
import com.example.harpapp.domain.model.LessonProgress
import com.example.harpapp.domain.model.LessonStatus
import java.security.MessageDigest

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
        updatedAt = System.currentTimeMillis(),
        lessonFingerprint = lesson.lessonFingerprint()
    )
}

fun Lesson.lessonFingerprint(): String {
    val canonicalLesson = steps.joinToString("\n") { step ->
        "${step.index}|${step.syllable.length}:${step.syllable}|${step.pitch.midi}"
    }
    return MessageDigest.getInstance("SHA-256")
        .digest(canonicalLesson.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
}
