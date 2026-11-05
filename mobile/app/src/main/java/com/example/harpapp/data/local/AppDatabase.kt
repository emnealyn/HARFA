package com.example.harpapp.data.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.harpapp.data.media.MidiDurationHelper
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.InputStreamReader

@Database(entities = [SongEntity::class], version = 14, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "harp_database"
                )
                    .fallbackToDestructiveMigration(true)
                    .addCallback(AppDatabaseCallback(scope, context.applicationContext))
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope,
        private val context: Context
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            populateDatabase()
        }

        override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
            super.onDestructiveMigration(db)
            populateDatabase()
        }

        private fun populateDatabase() {
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    seedDatabase(database.songDao())
                }
            }
        }

        private suspend fun seedDatabase(songDao: SongDao) {
            try {
                context.assets.open("songs.json").use { inputStream ->
                    InputStreamReader(inputStream).use { reader ->
                        val songListType = object : TypeToken<List<SongAsset>>() {}.type
                        val songs = Gson().fromJson<List<SongAsset>>(reader, songListType)
                            .map { it.toEntity(context) }
                        songDao.insertAllSongs(songs)
                    }
                }
            } catch (exception: Exception) {
                Log.e("AppDatabase", "Failed to seed songs database", exception)
            }
        }
    }
}

private data class SongAsset(
    val title: String = "",
    val artist: String = "",
    val difficulty: String = "EASY",
    val duration: String = "",
    val midiFilePath: String? = null,
    val coverImage: String? = null,
    @SerializedName("is_favorite")
    val isFavorite: Boolean = false,
    val lyricsWithNotes: List<LyricNoteAsset> = emptyList()
) {
    fun toEntity(context: Context): SongEntity {
        val calculatedDuration = midiFilePath?.let { path ->
            try {
                val durationMs = MidiDurationHelper.getDurationMs(context, path)
                MidiDurationHelper.formatDuration(durationMs)
            } catch (exception: Exception) {
                Log.w("AppDatabase", "Could not read MIDI duration for $path", exception)
                null
            }
        } ?: duration

        return SongEntity(
            title = title,
            artist = artist,
            difficulty = difficulty,
            duration = calculatedDuration,
            lyricsWithNotes = lyricsWithNotes.map {
                LyricNoteEntity(it.syllable, it.note)
            },
            midiFilePath = midiFilePath,
            coverImage = coverImage,
            isFavorite = isFavorite
        )
    }
}

private data class LyricNoteAsset(
    val syllable: String = "",
    val note: String = ""
)
