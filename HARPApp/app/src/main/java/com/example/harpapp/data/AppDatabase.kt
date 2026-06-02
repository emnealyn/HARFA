package com.example.harpapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.harpapp.model.Song
import com.example.harpapp.model.Difficulty
import com.example.harpapp.model.LyricNote
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

@Database(entities = [Song::class], version = 14, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "harp_database"
                )
                    .fallbackToDestructiveMigration(true)
                    .addCallback(AppDatabaseCallback(scope, context.applicationContext))
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope,
        private val context: Context
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    prepopulateDatabase(database.songDao())
                }
            }
        }

        override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
            super.onDestructiveMigration(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    prepopulateDatabase(database.songDao())
                }
            }
        }

        private suspend fun prepopulateDatabase(songDao: SongDao) {
            try {
                val inputStream = context.assets.open("songs.json")
                val reader = InputStreamReader(inputStream)

                val songListType = object : TypeToken<List<Song>>() {}.type

                val initialSongs: List<Song> = Gson().fromJson(reader, songListType)

                initialSongs.forEach { song ->
                    song.midiFilePath?.let { path ->
                        try {
                            val durationMs = MidiDurationHelper.getDurationMs(context, path)
                            song.duration = MidiDurationHelper.formatDuration(durationMs)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                songDao.insertAllSongs(initialSongs)

                reader.close()
                inputStream.close()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}