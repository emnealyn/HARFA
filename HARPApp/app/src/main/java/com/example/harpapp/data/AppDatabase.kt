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

@Database(entities = [Song::class], version = 1, exportSchema = false)
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
                    .fallbackToDestructiveMigration()
                    .addCallback(AppDatabaseCallback(scope))
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope,
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    prepopulateDatabase(database.songDao())
                }
            }
        }

        suspend fun prepopulateDatabase(songDao: SongDao) {
            val initialSongs = listOf(
                Song(
                    title = "Wildest Dreams",
                    artist = "Taylor Swift",
                    difficulty = Difficulty.MEDIUM,
                    duration = "2:55",
                    lyricsWithNotes = """
                        He said, "Let's get out of this town"
                        D  D  A C D  FF#  D A C
                        Drive out of the city, away from the crowds"
                        F  A  C  D  F#  F
                        I thought Heaven can't help me now
                        D  D  A  D
                        Nothing lasts forever
                        D  F#  C  C  C
                        But this is gonna take me down
                        FFD  CCCC
                        He's so tall and handsome as hell
                        A  A  C  D
                        He's so bad, but he does it so well
                        I can see the end as it begins
                        My one condition is
                    """.trimIndent()
                ),
                Song(
                    title = "Let It Go",
                    artist = "Idina Menzel",
                    difficulty = Difficulty.EASY,
                    duration = "3:45",
                    lyricsWithNotes = "The snow glows white on the mountain tonight..."
                ),
                Song(
                    title = "My Heart Will Go On",
                    artist = "Celine Dion",
                    difficulty = Difficulty.HARD,
                    duration = "4:20",
                    lyricsWithNotes = "Every night in my dreams I see you, I feel you..."
                ),
                Song(
                    title = "A Thousand Years",
                    artist = "Christina Perri",
                    difficulty = Difficulty.MEDIUM,
                    duration = "4:45",
                    lyricsWithNotes = "Heart beats fast, colors and promises..."
                )
            )

            songDao.insertAllSongs(initialSongs)
        }
    }
}