package com.example.harpapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.harpapp.domain.model.ConnectionState
import com.example.harpapp.domain.model.PairedDevice
import com.example.harpapp.domain.repository.InstrumentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BluetoothViewModel(
    private val instrumentRepository: InstrumentRepository
) : ViewModel() {

    val connectionState: Flow<ConnectionState> = instrumentRepository.connectionState
    val isConnected: StateFlow<Boolean> = instrumentRepository.connectionState
        .map { state -> state == ConnectionState.CONNECTED }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )
    val connectedDeviceName: Flow<String?> = instrumentRepository.connectedDeviceName
    val pairedDevices: Flow<List<PairedDevice>> = instrumentRepository.pairedDevices

    fun fetchPairedDevices() {
        viewModelScope.launch {
            instrumentRepository.refreshPairedDevices()
        }
    }

    fun connectToDevice(device: PairedDevice) {
        viewModelScope.launch {
            instrumentRepository.connect(device)
        }
    }

    fun disconnect() {
        instrumentRepository.disconnect()
    }

    override fun onCleared() {
        instrumentRepository.disconnect()
        super.onCleared()
    }
}
