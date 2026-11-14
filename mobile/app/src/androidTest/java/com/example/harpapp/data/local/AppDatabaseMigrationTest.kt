package com.example.harpapp.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    private val context: Context =
        InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        context.deleteDatabase(TEST_DATABASE)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(TEST_DATABASE)
    }

    @Test
    fun migrate14To17_preservesFavoriteSongs() {
        createLegacyDatabase(14) {
            execSQL(
                """
                CREATE TABLE songs (
                    id INTEGER NOT NULL,
                    isFavorite INTEGER NOT NULL,
                    PRIMARY KEY(id)
                )
                """.trimIndent()
            )
            execSQL("INSERT INTO songs(id, isFavorite) VALUES (1, 1)")
            execSQL("INSERT INTO songs(id, isFavorite) VALUES (2, 0)")
        }

        val migratedDatabase = openMigratedDatabase(AppDatabase.MIGRATION_14_17)

        try {
            migratedDatabase.openHelper.writableDatabase.query(
                "SELECT songId FROM favorite_songs ORDER BY songId"
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("another-love", cursor.getString(0))
                assertTrue(cursor.isLast)
            }
        } finally {
            migratedDatabase.close()
        }
    }

    @Test
    fun migrate15To17_preservesFavoriteSongs() {
        createLegacyDatabase(15) {
            execSQL(
                """
                CREATE TABLE songs (
                    id TEXT NOT NULL,
                    isFavorite INTEGER NOT NULL,
                    PRIMARY KEY(id)
                )
                """.trimIndent()
            )
            execSQL("INSERT INTO songs(id, isFavorite) VALUES ('song-a', 1)")
            execSQL("INSERT INTO songs(id, isFavorite) VALUES ('song-b', 0)")
        }

        val migratedDatabase = openMigratedDatabase(AppDatabase.MIGRATION_15_17)

        try {
            migratedDatabase.openHelper.writableDatabase.query(
                "SELECT songId FROM favorite_songs ORDER BY songId"
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("song-a", cursor.getString(0))
                assertTrue(cursor.isLast)
            }
        } finally {
            migratedDatabase.close()
        }
    }

    @Test
    fun migrate16To17_addsLessonFingerprintWithoutLosingProgress() {
        createLegacyDatabase(16) {
            execSQL(
                """
                CREATE TABLE favorite_songs (
                    songId TEXT NOT NULL,
                    addedAt INTEGER NOT NULL,
                    PRIMARY KEY(songId)
                )
                """.trimIndent()
            )
            execSQL(
                """
                CREATE TABLE song_progress (
                    songId TEXT NOT NULL,
                    currentIndex INTEGER NOT NULL,
                    hits INTEGER NOT NULL,
                    misses INTEGER NOT NULL,
                    status TEXT NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    PRIMARY KEY(songId)
                )
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO song_progress(
                    songId, currentIndex, hits, misses, status, updatedAt
                ) VALUES ('song-a', 3, 5, 1, 'IN_PROGRESS', 1234)
                """.trimIndent()
            )
        }

        val migratedDatabase = openMigratedDatabase(AppDatabase.MIGRATION_16_17)

        try {
            migratedDatabase.openHelper.writableDatabase.query(
                """
                SELECT currentIndex, hits, misses, status, lessonFingerprint
                FROM song_progress
                WHERE songId = 'song-a'
                """.trimIndent()
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(3, cursor.getInt(0))
                assertEquals(5, cursor.getInt(1))
                assertEquals(1, cursor.getInt(2))
                assertEquals("IN_PROGRESS", cursor.getString(3))
                assertEquals("", cursor.getString(4))
            }
        } finally {
            migratedDatabase.close()
        }
    }

    private fun createLegacyDatabase(
        version: Int,
        createSchema: SupportSQLiteDatabase.() -> Unit
    ) {
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(TEST_DATABASE)
            .callback(
                object : SupportSQLiteOpenHelper.Callback(version) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.createSchema()
                    }

                    override fun onUpgrade(
                        db: SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int
                    ) = Unit
                }
            )
            .build()
        FrameworkSQLiteOpenHelperFactory()
            .create(configuration)
            .use { it.writableDatabase }
    }

    private fun openMigratedDatabase(vararg migrations: Migration): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, TEST_DATABASE)
            .allowMainThreadQueries()
            .addMigrations(*migrations)
            .build()

    private companion object {
        const val TEST_DATABASE = "migration-test"
    }
}
