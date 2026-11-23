package com.example.harpapp.data.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.StringReader

class SongCatalogParserTest {

    private fun parse(json: String) = SongCatalogParser.parse(StringReader(json), "test.json")

    @Test
    fun `parser zachowuje tuningId utworu`() {
        val songs = parse(
            """[{"id":"a","title":"A","tuningId":"e-minor","lyricsWithNotes":[{"syllable":"la","note":"E4"}]}]"""
        )

        assertEquals("e-minor", songs.single().tuningId)
    }

    @Test
    fun `brak lub pusty tuningId daje null`() {
        val songs = parse(
            """[
              {"id":"a","title":"A","lyricsWithNotes":[]},
              {"id":"b","title":"B","tuningId":"  ","lyricsWithNotes":[]}
            ]"""
        )

        assertNull(songs[0].tuningId)
        assertNull(songs[1].tuningId)
    }

    /** Regresja: merge HARFA-20 gubil tuningId, przez co ekran harfy nie pokazywal flag ani oktawy. */
    @Test
    fun `kazdy utwor z bundlowanego katalogu ma tuningId`() {
        val songs = File("src/main/assets/songs.json").reader(Charsets.UTF_8).use {
            SongCatalogParser.parse(it, "songs.json")
        }

        assertTrue(songs.isNotEmpty())
        songs.forEach { song ->
            assertTrue("${song.id} nie ma tuningId", !song.tuningId.isNullOrBlank())
        }
    }
}
