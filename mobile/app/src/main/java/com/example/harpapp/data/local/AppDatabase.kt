package com.example.harpapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [FavoriteSongEntity::class, SongProgressEntity::class],
    version = 17,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun progressDao(): ProgressDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "harp_database"
                )
                    .addMigrations(
                        MIGRATION_14_17,
                        MIGRATION_15_17,
                        MIGRATION_16_17
                    )
                    .build()
                    .also { INSTANCE = it }
            }
        }

        private val MIGRATION_14_17 = object : Migration(14, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                createUserStateTables(db)

                db.query("SELECT id FROM songs WHERE isFavorite = 1").use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow("id")
                    while (cursor.moveToNext()) {
                        val legacyId = cursor.getInt(idColumn)
                        val songId = LEGACY_SONG_IDS.getOrNull(legacyId - 1) ?: continue
                        insertFavorite(db, songId)
                    }
                }

                db.execSQL("DROP TABLE IF EXISTS songs")
            }
        }

        private val MIGRATION_15_17 = object : Migration(15, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                createUserStateTables(db)
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO favorite_songs(songId, addedAt)
                    SELECT id, CAST(strftime('%s', 'now') AS INTEGER) * 1000
                    FROM songs
                    WHERE isFavorite = 1
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE IF EXISTS songs")
            }
        }

        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    ALTER TABLE song_progress
                    ADD COLUMN lessonFingerprint TEXT NOT NULL DEFAULT ''
                    """.trimIndent()
                )
            }
        }
    }
}

private val LEGACY_SONG_IDS = listOf(
    "another-love",
    "wildest-dreams",
    "lovely",
    "hallelujah",
    "viva-la-test"
)

private fun createUserStateTables(db: SupportSQLiteDatabase) {
    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS favorite_songs (
            songId TEXT NOT NULL,
            addedAt INTEGER NOT NULL,
            PRIMARY KEY(songId)
        )
        """.trimIndent()
    )
    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS song_progress (
            songId TEXT NOT NULL,
            currentIndex INTEGER NOT NULL,
            hits INTEGER NOT NULL,
            misses INTEGER NOT NULL,
            status TEXT NOT NULL,
            updatedAt INTEGER NOT NULL,
            lessonFingerprint TEXT NOT NULL DEFAULT '',
            PRIMARY KEY(songId)
        )
        """.trimIndent()
    )
}

private fun insertFavorite(db: SupportSQLiteDatabase, songId: String) {
    db.execSQL(
        "INSERT OR IGNORE INTO favorite_songs(songId, addedAt) VALUES (?, ?)",
        arrayOf<Any>(songId, System.currentTimeMillis())
    )
}
