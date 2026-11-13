package com.example.harpapp.data.local

import android.content.Context
import android.util.Log
import com.example.harpapp.data.catalog.dto.SongCatalogDto
import com.example.harpapp.domain.model.Song
import com.example.harpapp.domain.repository.SongRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.InputStreamReader

class RoomSongRepository(
    private val context: Context,
    private val favoriteDao: FavoriteDao
) : SongRepository {

    private fun loadCatalogFromAssets(): List<SongCatalogDto> {
        return try {
            context.assets.open("songs.json").use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    val type = object : TypeToken<List<SongCatalogDto>>() {}.type
                    Gson().fromJson(reader, type) ?: emptyList()
                }
            }
        } catch (exception: Exception) {
            Log.e("RoomSongRepository", "Failed to load songs from assets", exception)
            emptyList()
        }
    }

    override fun observeSongs(): Flow<List<Song>> {
        return favoriteDao.observeFavoriteSongIds().map { favoriteIds ->
            val favoriteSet = favoriteIds.toSet()
            loadCatalogFromAssets().map { dto ->
                dto.toDomain(isFavorite = favoriteSet.contains(dto.id))
            }
        }
    }

    override suspend fun getSong(songId: String): Song? {
        val dto = loadCatalogFromAssets().firstOrNull { it.id == songId } ?: return null
        val isFav = favoriteDao.isFavorite(songId)
        return dto.toDomain(isFavorite = isFav)
    }

    override suspend fun setFavorite(songId: String, isFavorite: Boolean) {
        if (isFavorite) {
            favoriteDao.insertFavorite(FavoriteSongEntity(songId))
        } else {
            favoriteDao.deleteFavorite(songId)
        }
    }
}
