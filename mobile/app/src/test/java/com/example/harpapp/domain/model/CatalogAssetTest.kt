package com.example.harpapp.domain.model

import com.example.harpapp.data.catalog.dto.TuningCatalogDto
import com.example.harpapp.data.catalog.toDomain
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Walidacja rzeczywistego katalogu `src/main/assets/songs.json`. */
class CatalogAssetTest {

    private class SongJson(
        val id: String?,
        val title: String?,
        val tuningId: String?,
        val lyricsWithNotes: List<CatalogStep?>?
    )

    private val songs: List<SongJson> = File("src/main/assets/songs.json").reader(Charsets.UTF_8).use {
        Gson().fromJson(it, object : TypeToken<List<SongJson>>() {}.type)
    }

    private val tuningDtos: List<TuningCatalogDto> = File("src/main/assets/tunings.json").reader(Charsets.UTF_8).use {
        Gson().fromJson(it, object : TypeToken<List<TuningCatalogDto>>() {}.type)
    }

    private val tunings: Map<String, Tuning> = tuningDtos.mapNotNull { it.toDomain() }.associateBy { it.id }

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

    @Test
    fun `wszystkie strojenia w katalogu sa poprawne i maja unikalne id`() {
        assertTrue(tuningDtos.isNotEmpty())
        tuningDtos.forEach { assertNotNull(it.id, it.toDomain()) }
        assertEquals(tuningDtos.size, tuningDtos.map { it.id }.toSet().size)
    }

    @Test
    fun `kazdy utwor ma strojenie, przy ktorym da sie go zagrac`() {
        songs.forEach { song ->
            val tuning = tunings[song.tuningId]
            assertNotNull("${song.title}: brak strojenia ${song.tuningId}", tuning)

            val pitches = song.lyricsWithNotes.orEmpty()
                .mapNotNull { (Pitch.parse(it?.note) as? PitchParseResult.Valid)?.pitch }
            val playable = OctaveShift.entries.any { shift ->
                val state = tuning!!.toInstrumentState().copy(octaveShift = shift)
                pitches.all { state.stringFor(it) != null }
            }
            assertTrue("${song.title}: nie da sie zagrac przy ${song.tuningId}", playable)
        }
    }
}
