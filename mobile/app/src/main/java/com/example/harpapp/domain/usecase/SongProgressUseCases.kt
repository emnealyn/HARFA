package com.example.harpapp.domain.usecase

import com.example.harpapp.domain.model.LessonProgress
import com.example.harpapp.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow

class ObserveSongProgressUseCase(
    private val repository: SongRepository
) {
    operator fun invoke(songId: String): Flow<LessonProgress?> = repository.observeProgress(songId)
}

class GetSongProgressUseCase(
    private val repository: SongRepository
) {
    suspend operator fun invoke(songId: String): LessonProgress? = repository.getProgress(songId)
}

class SaveSongProgressUseCase(
    private val repository: SongRepository
) {
    suspend operator fun invoke(progress: LessonProgress) {
        repository.saveProgress(progress)
    }
}

class DeleteSongProgressUseCase(
    private val repository: SongRepository
) {
    suspend operator fun invoke(songId: String) {
        repository.deleteProgress(songId)
    }
}
