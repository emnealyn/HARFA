package com.example.harpapp.domain.model

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Walidacja rzeczywistego katalogu `src/main/assets/songs.json`. */
class CatalogAssetTest {

    private class SongJson(val id: String?, val title: String?, val lyricsWithNotes: List<CatalogStep?>?)

    private val songs: List<SongJson> = File("src/main/assets/songs.json").reader(Charsets.UTF_8).use {
        Gson().fromJson(it, object : TypeToken<List<SongJson>>() {}.type)
    }

    private fun validateAll(): Map<String?, LessonValidation> =
        songs.mapIndexed { i, song ->
            song.title to LessonValidator.validate(song.id ?: i.toString(), song.title, song.lyricsWithNotes)
        }.toMap()

    @Test
    fun `walidacja calego katalogu nie zglasza wyjatku`() {
        assertTrue(songs.isNotEmpty())
        assertEquals(songs.size, validateAll().size)
    }

    @Test
    fun `wszystkie utwory w katalogu sa poprawne`() {
        validateAll().forEach { (title, result) ->
            assertTrue("$title: $result", result is LessonValidation.Valid)
        }
    }
}
