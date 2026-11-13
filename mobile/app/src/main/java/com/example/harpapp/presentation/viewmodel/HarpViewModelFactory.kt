package com.example.harpapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.harpapp.domain.repository.InstrumentRepository
import com.example.harpapp.domain.usecase.AnalyzeTuningUseCase
import com.example.harpapp.domain.usecase.GetSongTuningUseCase
import com.example.harpapp.domain.usecase.GetSongUseCase
import com.example.harpapp.domain.usecase.GetTuningsUseCase

class HarpViewModelFactory(
    private val instrumentRepository: InstrumentRepository,
    private val getSongUseCase: GetSongUseCase,
    private val getSongTuningUseCase: GetSongTuningUseCase,
    private val analyzeTuningUseCase: AnalyzeTuningUseCase,
    private val getTuningsUseCase: GetTuningsUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HarpViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HarpViewModel(
                instrumentRepository = instrumentRepository,
                getSongUseCase = getSongUseCase,
                getSongTuningUseCase = getSongTuningUseCase,
                analyzeTuningUseCase = analyzeTuningUseCase,
                getTuningsUseCase = getTuningsUseCase
            ) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
