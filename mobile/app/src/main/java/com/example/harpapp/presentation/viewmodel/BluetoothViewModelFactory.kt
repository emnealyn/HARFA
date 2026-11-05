package com.example.harpapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.harpapp.domain.repository.InstrumentRepository

class BluetoothViewModelFactory(
    private val instrumentRepository: InstrumentRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BluetoothViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BluetoothViewModel(instrumentRepository) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
