package com.example.harpapp.domain.repository

import com.example.harpapp.domain.model.LessonProgress
import com.example.harpapp.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface SongRepository {
    fun observeSongs(): Flow<List<Song>>

    suspend fun getSong(songId: String): Song?

    suspend fun setFavorite(songId: String, isFavorite: Boolean)

    fun observeProgress(songId: String): Flow<LessonProgress?>

    suspend fun getProgress(songId: String): LessonProgress?

    suspend fun saveProgress(progress: LessonProgress)

    suspend fun deleteProgress(songId: String)
}
