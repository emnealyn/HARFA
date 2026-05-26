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

@Database(entities = [Song::class], version = 5, exportSchema = false)
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

        suspend fun prepopulateDatabase(songDao: SongDao) {
            val initialSongs = listOf(
                Song(
                    title = "Another Love",
                    artist = "Tom Odell",
                    difficulty = Difficulty.MEDIUM,
                    duration = MidiDurationHelper.formatDuration(MidiDurationHelper.getDurationMs(context, "midi/ANOTHERLOVE.MID")),
                    midiFilePath = "midi/ANOTHERLOVE.MID",
                    lyricsWithNotes = listOf(
                        LyricNote("I", "G4"),
                        LyricNote("wan-", "G4"),
                        LyricNote("na", "G4"),
                        LyricNote("take", "G4"),
                        LyricNote("you", "A4"),
                        LyricNote("some-", "B4"),
                        LyricNote("where", "G4"),
                        LyricNote("so", "G4"),
                        LyricNote("you", "G4"),
                        LyricNote("know", "C5"),
                        LyricNote("I", "C5"),
                        LyricNote("care", "B4"),
                        LyricNote("But", "G4"),
                        LyricNote("it's", "G4"),
                        LyricNote("so", "A4"),
                        LyricNote("cold", "G4"),
                        LyricNote("and", "F#4"),
                        LyricNote("I", "F#4"),
                        LyricNote("don't", "F#4"),
                        LyricNote("know", "F#4"),
                        LyricNote("where.", "E4"),
                        LyricNote("I", "G4"),
                        LyricNote("brought", "G4"),
                        LyricNote("you", "A4"),
                        LyricNote("da-", "B4"),
                        LyricNote("ffo-", "A4"),
                        LyricNote("dils", "G4"),
                        LyricNote("in", "G4"),
                        LyricNote("a", "G4"),
                        LyricNote("pre-", "C5"),
                        LyricNote("tty", "C5"),
                        LyricNote("string", "B4"),
                        LyricNote("but", "G4"),
                        LyricNote("they", "G4"),
                        LyricNote("won't", "A4"),
                        LyricNote("flower", "F#4"),
                        LyricNote("like", "F#4"),
                        LyricNote("they", "E4"),
                        LyricNote("did", "F#4"),
                        LyricNote("last", "F#4"),
                        LyricNote("spring.", "E4" )
                    )
                ),
                Song(
                    title = "Let It Go",
                    artist = "Idina Menzel",
                    difficulty = Difficulty.EASY,
                    duration = MidiDurationHelper.formatDuration(MidiDurationHelper.getDurationMs(context, "midi/ANOTHERLOVE.MID")),
                    midiFilePath = "midi/ANOTHERLOVE.MID",
                    lyricsWithNotes = listOf(
                        LyricNote("I", "G4"),
                        LyricNote("wan-", "G4"),
                        LyricNote("na", "G4"),
                        LyricNote("take", "G4"),
                        LyricNote("you", "A4"),
                        LyricNote("some-", "B4"),
                        LyricNote("where", "G4"),
                        LyricNote("so", "G4"),
                        LyricNote("you", "G4"),
                        LyricNote("know", "C5"),
                        LyricNote("I", "C5"),
                        LyricNote("care", "B4"),
                        LyricNote("But", "G4"),
                        LyricNote("it's", "G4"),
                        LyricNote("so", "A4"),
                        LyricNote("cold", "G4"),
                        LyricNote("and", "F#4"),
                        LyricNote("I", "F#4"),
                        LyricNote("don't", "F#4"),
                        LyricNote("know", "F#4"),
                        LyricNote("where", "E4")
                    )
                ),
                Song(
                    title = "My Heart Will Go On",
                    artist = "Celine Dion",
                    difficulty = Difficulty.HARD,
                    duration = MidiDurationHelper.formatDuration(MidiDurationHelper.getDurationMs(context, "midi/ANOTHERLOVE.MID")),
                    midiFilePath = "midi/ANOTHERLOVE.MID",
                    lyricsWithNotes = listOf(
                        LyricNote("I", "G4"),
                        LyricNote("wan-", "G4"),
                        LyricNote("na", "G4"),
                        LyricNote("take", "G4"),
                        LyricNote("you", "A4"),
                        LyricNote("some-", "B4"),
                        LyricNote("where", "G4"),
                        LyricNote("so", "G4"),
                        LyricNote("you", "G4"),
                        LyricNote("know", "C5"),
                        LyricNote("I", "C5"),
                        LyricNote("care", "B4"),
                        LyricNote("But", "G4"),
                        LyricNote("it's", "G4"),
                        LyricNote("so", "A4"),
                        LyricNote("cold", "G4"),
                        LyricNote("and", "F#4"),
                        LyricNote("I", "F#4"),
                        LyricNote("don't", "F#4"),
                        LyricNote("know", "F#4"),
                        LyricNote("where", "E4")
                    )
                ),
                Song(
                    title = "A Thousand Years",
                    artist = "Christina Perri",
                    difficulty = Difficulty.MEDIUM,
                    duration = MidiDurationHelper.formatDuration(MidiDurationHelper.getDurationMs(context, "midi/ANOTHERLOVE.MID")),
                    midiFilePath = "midi/ANOTHERLOVE.MID",
                    lyricsWithNotes = listOf(
                        LyricNote("I", "G4"),
                        LyricNote("wan-", "G4"),
                        LyricNote("na", "G4"),
                        LyricNote("take", "G4"),
                        LyricNote("you", "A4"),
                        LyricNote("some-", "B4"),
                        LyricNote("where", "G4"),
                        LyricNote("so", "G4"),
                        LyricNote("you", "G4"),
                        LyricNote("know", "C5"),
                        LyricNote("I", "C5"),
                        LyricNote("care", "B4"),
                        LyricNote("But", "G4"),
                        LyricNote("it's", "G4"),
                        LyricNote("so", "A4"),
                        LyricNote("cold", "G4"),
                        LyricNote("and", "F#4"),
                        LyricNote("I", "F#4"),
                        LyricNote("don't", "F#4"),
                        LyricNote("know", "F#4"),
                        LyricNote("where", "E4")
                    )
                )
            )

            songDao.insertAllSongs(initialSongs)
        }
    }
}
