package com.example.harpapp.data.repository

import com.example.harpapp.data.catalog.SongCatalogDataSource
import com.example.harpapp.data.catalog.dto.SongCatalogDto
import com.example.harpapp.data.local.FavoriteDao
import com.example.harpapp.data.local.FavoriteSongEntity
import com.example.harpapp.data.local.ProgressDao
import com.example.harpapp.data.local.SongProgressEntity
import com.example.harpapp.data.catalog.dto.LyricNoteDto
import com.example.harpapp.domain.model.CatalogStep
import com.example.harpapp.domain.model.LessonProgress
import com.example.harpapp.domain.model.LessonValidation
import com.example.harpapp.domain.model.LessonValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SongRepositoryImplTest {

    private class FakeCatalogDataSource(
        initialSongs: List<SongCatalogDto>
    ) : SongCatalogDataSource {
        var catalogSongs = initialSongs
        override fun getSongs(): List<SongCatalogDto> = catalogSongs
        override fun getSong(songId: String): SongCatalogDto? = catalogSongs.firstOrNull { it.id == songId }
    }

    private class FakeFavoriteDao : FavoriteDao {
        private val favorites = MutableStateFlow<Set<String>>(emptySet())

        override fun observeFavoriteSongIds(): Flow<List<String>> =
            favorites.map { it.toList() }

        override suspend fun isFavorite(songId: String): Boolean = favorites.value.contains(songId)

        override fun observeIsFavorite(songId: String): Flow<Boolean> =
            favorites.map { it.contains(songId) }

        override suspend fun insertFavorite(favorite: FavoriteSongEntity) {
            favorites.update { it + favorite.songId }
        }

        override suspend fun deleteFavorite(songId: String) {
            favorites.update { it - songId }
        }
    }

    private class FakeProgressDao : ProgressDao {
        private val progressMap = mutableMapOf<String, SongProgressEntity>()

        override suspend fun getProgress(songId: String): SongProgressEntity? = progressMap[songId]

        override fun observeProgress(songId: String): Flow<SongProgressEntity?> =
            MutableStateFlow(progressMap[songId])

        override fun observeAllProgress(): Flow<List<SongProgressEntity>> =
            MutableStateFlow(progressMap.values.toList())

        override suspend fun saveProgress(progress: SongProgressEntity) {
            progressMap[progress.songId] = progress
        }

        override suspend fun deleteProgress(songId: String) {
            progressMap.remove(songId)
        }
    }

    @Test
    fun `zmiana katalogu nie usuwa ulubionych ani postepu uzytkownika`() = runTest {
        val lyrics = listOf(
            LyricNoteDto("a", "G4"),
            LyricNoteDto("b", "A4")
        )
        val songDto1 = SongCatalogDto(
            id = "song-1",
            title = "Song 1",
            difficulty = "EASY",
            lyricsWithNotes = lyrics
        )
        val catalogDataSource = FakeCatalogDataSource(listOf(songDto1))
        val favoriteDao = FakeFavoriteDao()
        val progressDao = FakeProgressDao()

        val repository = SongRepositoryImpl(catalogDataSource, favoriteDao, progressDao)
        val progress = lessonProgress()

        // Uzytkownik dodaje do ulubionych
        repository.setFavorite("song-1", true)
        repository.saveProgress(progress)
        val songsBefore = repository.observeSongs().first()
        assertTrue(songsBefore[0].isFavorite)

        // Podmiana katalogu (nowa wersja katalogu z innym tytulem i nowym utworem)
        val updatedSongDto1 = songDto1.copy(
            title = "Updated Song 1",
            difficulty = "MEDIUM"
        )
        val songDto2 = SongCatalogDto(id = "song-2", title = "Song 2", difficulty = "HARD")
        catalogDataSource.catalogSongs = listOf(updatedSongDto1, songDto2)

        // Sprawdzenie czy stan uzytkownika przetrwal zmiane katalogu
        val songsAfter = repository.observeSongs().first()
        val song1After = songsAfter.first { it.id == "song-1" }
        assertTrue("Ulubione musza zostac zachowane po zmianie katalogu", song1After.isFavorite)
        assertEquals("Updated Song 1", song1After.title)

        val progressAfter = repository.getProgress("song-1")
        assertNotNull(progressAfter)
        assertEquals(progress.currentIndex, progressAfter?.currentIndex)
    }

    @Test
    fun `zmiana nut nie odtwarza niekompatybilnego postepu`() = runTest {
        val catalogDataSource = FakeCatalogDataSource(
            listOf(
                SongCatalogDto(
                    id = "song-1",
                    title = "Song 1",
                    lyricsWithNotes = listOf(
                        LyricNoteDto("a", "G4"),
                        LyricNoteDto("b", "A4")
                    )
                )
            )
        )
        val progressDao = FakeProgressDao()
        val repository = SongRepositoryImpl(
            catalogDataSource = catalogDataSource,
            favoriteDao = FakeFavoriteDao(),
            progressDao = progressDao,
            logWarning = {}
        )

        repository.saveProgress(lessonProgress())
        catalogDataSource.catalogSongs = listOf(
            SongCatalogDto(
                id = "song-1",
                title = "Song 1",
                lyricsWithNotes = listOf(
                    LyricNoteDto("a", "G4"),
                    LyricNoteDto("b", "B4")
                )
            )
        )

        assertNull(repository.getProgress("song-1"))
    }

    private fun lessonProgress(): LessonProgress {
        val validation = LessonValidator.validate(
            songId = "song-1",
            title = "Song 1",
            steps = listOf(CatalogStep("a", "G4"), CatalogStep("b", "A4"))
        )
        return LessonProgress.start((validation as LessonValidation.Valid).lesson)
    }
}
