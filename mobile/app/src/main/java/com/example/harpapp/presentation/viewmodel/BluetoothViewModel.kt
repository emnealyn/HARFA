package com.example.harpapp.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.harpapp.domain.model.BatteryStatus
import com.example.harpapp.domain.model.ConnectionState
import com.example.harpapp.domain.model.InstrumentDevice
import com.example.harpapp.domain.model.isConnected
import com.example.harpapp.domain.repository.InstrumentRepository
import com.example.harpapp.domain.usecase.lowBatteryWarnings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BluetoothViewModel(
    private val instrumentRepository: InstrumentRepository
) : ViewModel() {

    val connectionState: Flow<ConnectionState> = instrumentRepository.connectionState
    val isConnected: StateFlow<Boolean> = instrumentRepository.connectionState
        .map { state -> state.isConnected }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )
    val connectedDeviceName: Flow<String?> = instrumentRepository.connectedDeviceName
    val connectedDeviceId: Flow<Long?> = instrumentRepository.connectedDeviceId
    val discoveredDevices: Flow<List<InstrumentDevice>> = instrumentRepository.discoveredDevices
    val batteryStatus: StateFlow<BatteryStatus?> = instrumentRepository.batteryStatus
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    private val _lowBatteryWarnings = Channel<Unit>(Channel.CONFLATED)
    val lowBatteryWarnings: Flow<Unit> = _lowBatteryWarnings.receiveAsFlow()

    init {
        viewModelScope.launch {
            instrumentRepository.batteryStatus.lowBatteryWarnings().collect {
                _lowBatteryWarnings.send(Unit)
            }
        }
    }

    fun scanForDevices() {
        viewModelScope.launch {
            try {
                instrumentRepository.scanForDevices()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.e(TAG, "Could not scan for Bluetooth instruments", exception)
            }
        }
    }

    fun connectToDevice(device: InstrumentDevice) {
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

    private companion object {
        const val TAG = "BluetoothViewModel"
    }
}
