package com.example.harpapp.data.repository

import android.util.Log
import com.example.harpapp.data.catalog.SongCatalogDataSource
import com.example.harpapp.data.local.FavoriteDao
import com.example.harpapp.data.local.FavoriteSongEntity
import com.example.harpapp.data.local.ProgressDao
import com.example.harpapp.data.local.SongProgressEntity
import com.example.harpapp.data.local.lessonFingerprint
import com.example.harpapp.data.local.toDomain
import com.example.harpapp.data.local.toEntity
import com.example.harpapp.domain.model.CatalogStep
import com.example.harpapp.domain.model.LessonProgress
import com.example.harpapp.domain.model.LessonStatus
import com.example.harpapp.domain.model.LessonValidation
import com.example.harpapp.domain.model.LessonValidator
import com.example.harpapp.domain.model.Song
import com.example.harpapp.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SongRepositoryImpl(
    private val catalogDataSource: SongCatalogDataSource,
    private val favoriteDao: FavoriteDao,
    private val progressDao: ProgressDao,
    private val logWarning: (String) -> Unit = { message -> Log.w(TAG, message) }
) : SongRepository {

    override fun observeSongs(): Flow<List<Song>> {
        return favoriteDao.observeFavoriteSongIds().map { favoriteIds ->
            val favoriteSet = favoriteIds.toSet()
            catalogDataSource.getSongs().map { dto ->
                dto.toDomain(isFavorite = favoriteSet.contains(dto.id))
            }
        }
    }

    override suspend fun getSong(songId: String): Song? {
        val dto = catalogDataSource.getSong(songId) ?: return null
        val isFav = favoriteDao.isFavorite(songId)
        return dto.toDomain(isFavorite = isFav)
    }

    override suspend fun setFavorite(songId: String, isFavorite: Boolean) {
        if (isFavorite) {
            favoriteDao.insertFavorite(FavoriteSongEntity(songId))
        } else {
            favoriteDao.deleteFavorite(songId)
        }
    }

    override fun observeProgress(songId: String): Flow<LessonProgress?> {
        return progressDao.observeProgress(songId).map { entity ->
            entity?.let { createLessonProgress(it) }
        }
    }

    override suspend fun getProgress(songId: String): LessonProgress? {
        val entity = progressDao.getProgress(songId) ?: return null
        return createLessonProgress(entity)
    }

    override suspend fun saveProgress(progress: LessonProgress) {
        progressDao.saveProgress(progress.toEntity())
    }

    override suspend fun deleteProgress(songId: String) {
        progressDao.deleteProgress(songId)
    }

    private fun createLessonProgress(entity: SongProgressEntity): LessonProgress? {
        val dto = catalogDataSource.getSong(entity.songId) ?: return null
        val steps = dto.lyricsWithNotes.map { CatalogStep(it.syllable, it.note) }
        val validation = LessonValidator.validate(dto.id, dto.title, steps)
        if (validation !is LessonValidation.Valid) return null

        val lesson = validation.lesson
        val status = entity.status.toLessonStatusOrNull()
        val indexIsValid = entity.currentIndex in 0..lesson.steps.size
        val statusIsValid = status != null &&
            (status == LessonStatus.COMPLETED) == (entity.currentIndex == lesson.steps.size)
        val fingerprintIsValid = entity.lessonFingerprint.isEmpty() ||
            entity.lessonFingerprint == lesson.lessonFingerprint()

        if (!indexIsValid || !statusIsValid || !fingerprintIsValid) {
            logWarning("Ignoring incompatible saved progress for songId=${entity.songId}")
            return null
        }

        return entity.toDomain(lesson)
    }

    private fun String.toLessonStatusOrNull(): LessonStatus? =
        try {
            LessonStatus.valueOf(this)
        } catch (_: IllegalArgumentException) {
            null
        }

    private companion object {
        const val TAG = "SongRepository"
    }
}
