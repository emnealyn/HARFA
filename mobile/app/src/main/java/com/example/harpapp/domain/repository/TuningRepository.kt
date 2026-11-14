package com.example.harpapp.domain.repository

import com.example.harpapp.domain.model.Tuning

interface TuningRepository {
    suspend fun getTuning(tuningId: String): Tuning?

    suspend fun getTunings(): List<Tuning>
}
