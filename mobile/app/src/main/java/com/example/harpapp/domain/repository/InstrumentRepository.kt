package com.example.harpapp.domain.repository

import com.example.harpapp.domain.model.ConnectionState
import com.example.harpapp.domain.model.InstrumentCapabilities
import com.example.harpapp.domain.model.InstrumentCommand
import com.example.harpapp.domain.model.InstrumentDevice
import com.example.harpapp.domain.model.InstrumentEvent
import com.example.harpapp.domain.model.InstrumentState
import com.example.harpapp.domain.model.NoteEvent
import kotlinx.coroutines.flow.Flow

interface InstrumentRepository {
    val connectionState: Flow<ConnectionState>
    val connectedDeviceName: Flow<String?>
    val connectedDeviceId: Flow<Long?>
    val discoveredDevices: Flow<List<InstrumentDevice>>

    /** Wszystkie zdarzenia harfy, w tym [NoteEvent] i [InstrumentEvent.ConfigurationUpdated]. */
    val events: Flow<InstrumentEvent>

    /** Zagrania strun zgłoszone przez harfę (`STRING_EVENT`, docs/domain-model.md §4). */
    val noteEvents: Flow<NoteEvent>

    /** Aktualne strojenie harfy (`CONFIG_STATE`). Przed pierwszą ramką: [InstrumentState.DEFAULT]. */
    val instrumentState: Flow<InstrumentState>

    val capabilities: Flow<InstrumentCapabilities>

    suspend fun scanForDevices()

    suspend fun connect(device: InstrumentDevice)

    suspend fun send(command: InstrumentCommand)

    fun disconnect()
}
