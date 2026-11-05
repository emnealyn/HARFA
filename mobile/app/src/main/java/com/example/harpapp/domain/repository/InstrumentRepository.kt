package com.example.harpapp.domain.repository

import com.example.harpapp.domain.model.ConnectionState
import com.example.harpapp.domain.model.InstrumentCommand
import com.example.harpapp.domain.model.InstrumentDevice
import com.example.harpapp.domain.model.InstrumentEvent
import com.example.harpapp.domain.model.NotePlayed
import kotlinx.coroutines.flow.Flow

interface InstrumentRepository {
    val connectionState: Flow<ConnectionState>
    val connectedDeviceName: Flow<String?>
    val discoveredDevices: Flow<List<InstrumentDevice>>
    val events: Flow<InstrumentEvent>
    val incomingNotes: Flow<NotePlayed>

    suspend fun scanForDevices()

    suspend fun connect(device: InstrumentDevice)

    suspend fun send(command: InstrumentCommand)

    fun disconnect()
}
