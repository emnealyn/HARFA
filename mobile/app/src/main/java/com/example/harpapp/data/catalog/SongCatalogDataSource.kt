package com.example.harpapp.data.catalog

import android.content.Context
import android.util.Log
import com.example.harpapp.data.catalog.dto.SongCatalogDto
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

interface SongCatalogDataSource {
    fun getSongs(): List<SongCatalogDto>
    fun getSong(songId: String): SongCatalogDto?
}

class AssetSongCatalogDataSource(
    private val context: Context,
    private val fileName: String = "songs.json"
) : SongCatalogDataSource {

    override fun getSongs(): List<SongCatalogDto> {
        return try {
            context.assets.open(fileName).use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    val type = object : TypeToken<List<SongCatalogDto>>() {}.type
                    Gson().fromJson(reader, type) ?: emptyList()
                }
            }
        } catch (exception: Exception) {
            Log.e("AssetCatalogDataSource", "Failed to load catalog from $fileName", exception)
            emptyList()
        }
    }

    override fun getSong(songId: String): SongCatalogDto? {
        return getSongs().firstOrNull { it.id == songId }
    }
}
