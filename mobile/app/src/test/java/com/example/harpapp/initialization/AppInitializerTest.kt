package com.example.harpapp.initialization

import com.example.harpapp.data.catalog.CatalogLoadException
import com.example.harpapp.data.catalog.SongCatalogDataSource
import com.example.harpapp.data.catalog.dto.SongCatalogDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppInitializerTest {

    @Test
    fun `poprawny katalog daje stan gotowy`() {
        val initializer = AppInitializer(FakeCatalogDataSource(catalogSongs = listOf(song())))

        assertEquals(AppInitializationState.Ready, initializer.initialize())
    }

    @Test
    fun `pusty katalog daje jawny stan bledu`() {
        val initializer = AppInitializer(FakeCatalogDataSource(catalogSongs = emptyList()))

        val state = initializer.initialize()

        assertTrue(state is AppInitializationState.Error)
        assertEquals("Song catalog is empty", (state as AppInitializationState.Error).message)
    }

    @Test
    fun `przerwana inicjalizacja moze zostac ponowiona`() {
        val source = FakeCatalogDataSource(
            catalogSongs = emptyList(),
            failure = CatalogLoadException("temporary failure")
        )
        val initializer = AppInitializer(source)

        val failedState = initializer.initialize()
        source.failure = null
        source.catalogSongs = listOf(song())

        assertTrue(failedState is AppInitializationState.Error)
        assertEquals(AppInitializationState.Ready, initializer.initialize())
    }

    private fun song() = SongCatalogDto(
        id = "song-1",
        title = "Song 1"
    )

    private class FakeCatalogDataSource(
        var catalogSongs: List<SongCatalogDto>,
        var failure: CatalogLoadException? = null
    ) : SongCatalogDataSource {
        override fun getSongs(): List<SongCatalogDto> =
            failure?.let { throw it } ?: catalogSongs

        override fun getSong(songId: String): SongCatalogDto? =
            getSongs().firstOrNull { it.id == songId }
    }
}
