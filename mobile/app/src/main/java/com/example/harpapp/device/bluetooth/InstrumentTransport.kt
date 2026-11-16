package com.example.harpapp.device.bluetooth

import com.example.harpapp.domain.model.InstrumentDevice
import kotlinx.coroutines.flow.Flow
import java.io.IOException

internal sealed interface TransportEvent {
    data class Data(val bytes: ByteArray) : TransportEvent

    data object Disconnected : TransportEvent
}

internal enum class TransportConnectionStage {
    CONNECTING,
    DISCOVERING_SERVICES,
    READY_FOR_HANDSHAKE
}

internal class BluetoothUnavailableException(
    message: String
) : IOException(message)

internal class BleConnectionTimeoutException(
    message: String
) : IOException(message)

internal class BleServiceDiscoveryException(
    message: String
) : IOException(message)

internal interface InstrumentTransport {
    val events: Flow<TransportEvent>

    suspend fun connect(
        device: InstrumentDevice,
        onStage: (TransportConnectionStage) -> Unit
    )

    suspend fun send(bytes: ByteArray)

    fun disconnect()
}
