package com.example.harpapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.harpapp.domain.repository.InstrumentRepository

class HarpViewModelFactory(
    private val instrumentRepository: InstrumentRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HarpViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HarpViewModel(instrumentRepository) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
