package com.example.harpapp.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "song_progress")
data class SongProgressEntity(
    @PrimaryKey
    val songId: String,
    val currentIndex: Int,
    val hits: Int,
    val misses: Int,
    val status: String,
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "''")
    val lessonFingerprint: String = ""
)
