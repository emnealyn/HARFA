package com.example.harpapp.data.repository

import android.util.Log
import com.example.harpapp.data.catalog.TuningCatalogDataSource
import com.example.harpapp.data.catalog.toDomain
import com.example.harpapp.domain.model.Tuning
import com.example.harpapp.domain.repository.TuningRepository

class TuningRepositoryImpl(
    private val catalogDataSource: TuningCatalogDataSource,
    private val logWarning: (String) -> Unit = { message -> Log.w(TAG, message) }
) : TuningRepository {

    override suspend fun getTuning(tuningId: String): Tuning? {
        val dto = catalogDataSource.getTuning(tuningId) ?: return null
        return dto.toDomain().also {
            if (it == null) logWarning("Ignoring invalid tuning in catalog: tuningId=$tuningId")
        }
    }

    override suspend fun getTunings(): List<Tuning> =
        catalogDataSource.getTunings().mapNotNull { dto ->
            dto.toDomain().also {
                if (it == null) logWarning("Ignoring invalid tuning in catalog: tuningId=${dto.id}")
            }
        }

    private companion object {
        const val TAG = "TuningRepository"
    }
}
