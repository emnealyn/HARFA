package com.example.harpapp.data.local

import com.example.harpapp.domain.model.Song
import com.example.harpapp.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomSongRepository(
    private val songDao: SongDao
) : SongRepository {
    override fun observeSongs(): Flow<List<Song>> {
        return songDao.getAllSongEntities().map { songs ->
            songs.map(SongEntity::toDomain)
        }
    }

    override suspend fun getSong(songId: String): Song? {
        return songDao.getSongEntityById(songId)?.toDomain()
    }

    override suspend fun setFavorite(songId: String, isFavorite: Boolean) {
        songDao.updateFavoriteStatus(songId, isFavorite)
    }
}
