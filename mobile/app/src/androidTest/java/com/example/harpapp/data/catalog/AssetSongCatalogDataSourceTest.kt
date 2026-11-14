package com.example.harpapp.data.catalog

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AssetSongCatalogDataSourceTest {

    private val context =
        InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun loadsBundledCatalog() {
        val songs = AssetSongCatalogDataSource(context).getSongs()

        assertTrue(songs.isNotEmpty())
        assertTrue(songs.all { it.id.isNotBlank() && it.title.isNotBlank() })
    }

    @Test(expected = CatalogLoadException::class)
    fun reportsMissingCatalogFile() {
        AssetSongCatalogDataSource(context, "missing-songs.json").getSongs()
    }
}
