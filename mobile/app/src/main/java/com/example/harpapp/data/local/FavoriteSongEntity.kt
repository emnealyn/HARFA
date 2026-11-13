package com.example.harpapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_songs")
data class FavoriteSongEntity(
    @PrimaryKey
    val songId: String,
    val addedAt: Long = System.currentTimeMillis()
)
