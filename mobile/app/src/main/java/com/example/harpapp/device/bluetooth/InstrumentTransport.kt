package com.example.harpapp.device.bluetooth

import com.example.harpapp.domain.model.InstrumentDevice
import kotlinx.coroutines.flow.Flow

internal sealed interface TransportEvent {
    data class Data(val bytes: ByteArray) : TransportEvent

    data object Disconnected : TransportEvent
}

internal interface InstrumentTransport {
    val events: Flow<TransportEvent>

    suspend fun connect(device: InstrumentDevice)

    suspend fun send(bytes: ByteArray)

    fun disconnect()
}
