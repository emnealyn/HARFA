package com.example.harpapp.domain.usecase

import com.example.harpapp.domain.model.Song
import com.example.harpapp.domain.model.Tuning
import com.example.harpapp.domain.repository.TuningRepository

class GetSongTuningUseCase(
    private val repository: TuningRepository
) {
    suspend operator fun invoke(song: Song): Tuning? =
        song.tuningId?.let { repository.getTuning(it) }
}
