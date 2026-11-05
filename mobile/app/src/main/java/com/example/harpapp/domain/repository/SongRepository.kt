package com.example.harpapp.domain.repository

import com.example.harpapp.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface SongRepository {
    fun observeSongs(): Flow<List<Song>>

    suspend fun getSong(songId: Int): Song?

    suspend fun setFavorite(songId: Int, isFavorite: Boolean)
}
