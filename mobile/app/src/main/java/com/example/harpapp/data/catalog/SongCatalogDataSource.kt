package com.example.harpapp.data.catalog

import android.content.Context
import com.example.harpapp.data.catalog.dto.LyricNoteDto
import com.example.harpapp.data.catalog.dto.SongCatalogDto
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import java.io.IOException
import java.io.InputStreamReader

interface SongCatalogDataSource {
    fun getSongs(): List<SongCatalogDto>
    fun getSong(songId: String): SongCatalogDto?
}

class CatalogLoadException(
    message: String,
    cause: Throwable? = null
) : IllegalStateException(message, cause)

class AssetSongCatalogDataSource(
    private val context: Context,
    private val fileName: String = "songs.json"
) : SongCatalogDataSource {

    private val loadedSongs: List<SongCatalogDto> by lazy {
        try {
            context.assets.open(fileName).use { inputStream ->
                InputStreamReader(inputStream, Charsets.UTF_8).use(::parseSongs)
            }
        } catch (exception: IOException) {
            throw CatalogLoadException(
                "Could not read catalog file $fileName",
                exception
            )
        } catch (exception: JsonParseException) {
            throw CatalogLoadException(
                "Could not parse catalog file $fileName",
                exception
            )
        }
    }

    override fun getSongs(): List<SongCatalogDto> = loadedSongs

    override fun getSong(songId: String): SongCatalogDto? =
        loadedSongs.firstOrNull { it.id == songId }

    private fun parseSongs(reader: InputStreamReader): List<SongCatalogDto> {
        val type = object : TypeToken<List<RawSongCatalogDto?>>() {}.type
        val rawSongs: List<RawSongCatalogDto?>? = Gson().fromJson(reader, type)

        if (rawSongs.isNullOrEmpty()) {
            throw CatalogLoadException("Catalog file $fileName is empty")
        }

        val ids = mutableSetOf<String>()
        return rawSongs.mapIndexed { index, rawSong ->
            val song = rawSong
                ?: throw CatalogLoadException("Catalog entry $index is null")
            val id = song.id.requireValue(index, "id")
            val title = song.title.requireValue(index, "title")

            if (!ids.add(id)) {
                throw CatalogLoadException("Catalog contains duplicate song id '$id'")
            }

            val lyricsWithNotes = song.lyricsWithNotes
                ?: throw CatalogLoadException(
                    "Catalog entry $index ('$id') has no lyricsWithNotes"
                )

            SongCatalogDto(
                id = id,
                title = title,
                artist = song.artist.orEmpty(),
                difficulty = song.difficulty ?: "EASY",
                duration = song.duration.orEmpty(),
                midiFilePath = song.midiFilePath,
                coverImage = song.coverImage,
                lyricsWithNotes = lyricsWithNotes.mapIndexed { noteIndex, rawNote ->
                    val note = rawNote
                        ?: throw CatalogLoadException(
                            "Catalog entry $index ('$id') contains a null lyric at index $noteIndex"
                        )
                    LyricNoteDto(
                        syllable = note.syllable.orEmpty(),
                        note = note.note.orEmpty()
                    )
                }
            )
        }
    }

    private fun String?.requireValue(index: Int, field: String): String =
        this?.takeIf { it.isNotBlank() }
            ?: throw CatalogLoadException("Catalog entry $index has no $field")

    private data class RawSongCatalogDto(
        val id: String?,
        val title: String?,
        val artist: String?,
        val difficulty: String?,
        val duration: String?,
        val midiFilePath: String?,
        val coverImage: String?,
        val lyricsWithNotes: List<RawLyricNoteDto?>?
    )

    private data class RawLyricNoteDto(
        val syllable: String?,
        val note: String?
    )
}
