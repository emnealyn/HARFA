package com.example.harpapp.data.catalog

import android.content.Context
import android.util.Log
import com.example.harpapp.data.catalog.dto.TuningCatalogDto
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

interface TuningCatalogDataSource {
    fun getTunings(): List<TuningCatalogDto>
    fun getTuning(tuningId: String): TuningCatalogDto?
}

class AssetTuningCatalogDataSource(
    private val context: Context,
    private val fileName: String = "tunings.json"
) : TuningCatalogDataSource {

    override fun getTunings(): List<TuningCatalogDto> {
        return try {
            context.assets.open(fileName).use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    val type = object : TypeToken<List<TuningCatalogDto>>() {}.type
                    Gson().fromJson(reader, type) ?: emptyList()
                }
            }
        } catch (exception: Exception) {
            Log.e("AssetTuningCatalog", "Failed to load tunings from $fileName", exception)
            emptyList()
        }
    }

    override fun getTuning(tuningId: String): TuningCatalogDto? {
        return getTunings().firstOrNull { it.id == tuningId }
    }
}
