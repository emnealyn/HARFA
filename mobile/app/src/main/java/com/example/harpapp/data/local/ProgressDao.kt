package com.example.harpapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {
    @Query("SELECT * FROM song_progress WHERE songId = :songId LIMIT 1")
    suspend fun getProgress(songId: String): SongProgressEntity?

    @Query("SELECT * FROM song_progress WHERE songId = :songId LIMIT 1")
    fun observeProgress(songId: String): Flow<SongProgressEntity?>

    @Query("SELECT * FROM song_progress")
    fun observeAllProgress(): Flow<List<SongProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: SongProgressEntity)

    @Query("DELETE FROM song_progress WHERE songId = :songId")
    suspend fun deleteProgress(songId: String)
}
