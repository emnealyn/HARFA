package com.example.harpapp.domain.repository

import com.example.harpapp.domain.model.ConnectionState
import com.example.harpapp.domain.model.NotePlayed
import com.example.harpapp.domain.model.PairedDevice
import kotlinx.coroutines.flow.Flow

interface InstrumentRepository {
    val connectionState: Flow<ConnectionState>
    val connectedDeviceName: Flow<String?>
    val pairedDevices: Flow<List<PairedDevice>>
    val incomingNotes: Flow<NotePlayed>

    suspend fun refreshPairedDevices()

    suspend fun connect(device: PairedDevice)

    fun disconnect()
}
