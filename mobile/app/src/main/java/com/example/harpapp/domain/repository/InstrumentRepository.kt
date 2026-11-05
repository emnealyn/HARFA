package com.example.harpapp.domain.repository

import com.example.harpapp.domain.model.ConnectionState
import com.example.harpapp.domain.model.InstrumentState
import com.example.harpapp.domain.model.NoteEvent
import com.example.harpapp.domain.model.PairedDevice
import kotlinx.coroutines.flow.Flow

interface InstrumentRepository {
    val connectionState: Flow<ConnectionState>
    val connectedDeviceName: Flow<String?>
    val pairedDevices: Flow<List<PairedDevice>>

    /** Zagrania strun zgłoszone przez harfę (`STRING_EVENT`, docs/domain-model.md §4). */
    val noteEvents: Flow<NoteEvent>

    /** Aktualne strojenie harfy (`CONFIG_STATE`). Przed pierwszą ramką: [InstrumentState.DEFAULT]. */
    val instrumentState: Flow<InstrumentState>

    suspend fun refreshPairedDevices()

    suspend fun connect(device: PairedDevice)

    fun disconnect()
}
