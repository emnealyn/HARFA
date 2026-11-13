package com.example.harpapp.data.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.harpapp.data.catalog.dto.SongCatalogDto
import com.example.harpapp.data.media.MidiDurationHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.InputStreamReader

@Database(entities = [SongEntity::class], version = 15, exportSchema = false)
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
                        val songListType = object : TypeToken<List<SongCatalogDto>>() {}.type
                        val dtos = Gson().fromJson<List<SongCatalogDto>>(reader, songListType)
                        val entities = dtos.map { dto ->
                            val calculatedDuration = dto.midiFilePath?.let { path ->
                                try {
                                    val durationMs = MidiDurationHelper.getDurationMs(context, path)
                                    MidiDurationHelper.formatDuration(durationMs)
                                } catch (exception: Exception) {
                                    Log.w("AppDatabase", "Could not read MIDI duration for $path", exception)
                                    null
                                }
                            } ?: dto.duration

                            SongEntity(
                                id = dto.id,
                                title = dto.title,
                                artist = dto.artist,
                                difficulty = dto.difficulty,
                                duration = calculatedDuration,
                                lyricsWithNotes = dto.lyricsWithNotes.map {
                                    LyricNoteEntity(it.syllable, it.note)
                                },
                                midiFilePath = dto.midiFilePath,
                                coverImage = dto.coverImage,
                                isFavorite = dto.isFavorite
                            )
                        }
                        songDao.insertAllSongs(entities)
                    }
                }
            } catch (exception: Exception) {
                Log.e("AppDatabase", "Failed to seed songs database", exception)
            }
        }
    }
}
