package com.example.harpapp.initialization

import com.example.harpapp.data.catalog.CatalogLoadException
import com.example.harpapp.data.catalog.SongCatalogDataSource

class AppInitializer(
    private val catalogDataSource: SongCatalogDataSource
) {
    fun initialize(): AppInitializationState {
        return try {
            if (catalogDataSource.getSongs().isEmpty()) {
                AppInitializationState.Error("Song catalog is empty")
            } else {
                AppInitializationState.Ready
            }
        } catch (exception: CatalogLoadException) {
            AppInitializationState.Error(
                exception.message ?: "Could not load song catalog"
            )
        }
    }
}
