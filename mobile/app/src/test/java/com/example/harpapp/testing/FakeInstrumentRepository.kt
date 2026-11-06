package com.example.harpapp.testing

import com.example.harpapp.domain.model.ConnectionState
import com.example.harpapp.domain.model.InstrumentCommand
import com.example.harpapp.domain.model.InstrumentDevice
import com.example.harpapp.domain.model.InstrumentEvent
import com.example.harpapp.domain.model.InstrumentState
import com.example.harpapp.domain.model.NoteEvent
import com.example.harpapp.domain.model.StringId
import com.example.harpapp.domain.repository.InstrumentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.ArrayDeque

class FakeInstrumentRepository : InstrumentRepository {

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: Flow<ConnectionState> = _connectionState.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    override val connectedDeviceName: Flow<String?> = _connectedDeviceName.asStateFlow()

    private val _connectedDeviceId = MutableStateFlow<Long?>(null)
    override val connectedDeviceId: Flow<Long?> = _connectedDeviceId.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<InstrumentDevice>>(emptyList())
    override val discoveredDevices: Flow<List<InstrumentDevice>> =
        _discoveredDevices.asStateFlow()

    private val _events = MutableSharedFlow<InstrumentEvent>(extraBufferCapacity = 16)
    override val events: Flow<InstrumentEvent> = _events

    private val _noteEvents = MutableSharedFlow<NoteEvent>(extraBufferCapacity = 16)
    override val noteEvents: Flow<NoteEvent> = _noteEvents

    private val _instrumentState = MutableStateFlow(InstrumentState.DEFAULT)
    override val instrumentState: Flow<InstrumentState> = _instrumentState.asStateFlow()

    private val recordedCommands = mutableListOf<InstrumentCommand>()
    private val scanFailures = ArrayDeque<Throwable>()
    private val connectFailures = ArrayDeque<Throwable>()
    private val commandFailures = ArrayDeque<Throwable>()

    val sentCommands: List<InstrumentCommand>
        get() = recordedCommands.toList()

    fun setDiscoveredDevices(devices: List<InstrumentDevice>) {
        _discoveredDevices.value = devices
    }

    fun failNextScan(error: Throwable) {
        scanFailures.addLast(error)
    }

    fun failNextConnect(error: Throwable) {
        connectFailures.addLast(error)
    }

    fun failNextCommand(error: Throwable) {
        commandFailures.addLast(error)
    }

    fun simulateConnectionState(
        state: ConnectionState,
        deviceName: String? = _connectedDeviceName.value
    ) {
        _connectionState.value = state
        if (state == ConnectionState.CONNECTED) {
            _connectedDeviceName.value = deviceName
        }
    }

    suspend fun emitEvent(event: InstrumentEvent) {
        _events.emit(event)
        when (event) {
            is NoteEvent -> _noteEvents.emit(event)
            is InstrumentEvent.ConfigurationUpdated -> _instrumentState.value = event.state
            else -> Unit
        }
    }

    suspend fun emitStringEvent(stringId: Int, midi: Int, timestampMs: Int) {
        emitEvent(
            NoteEvent(
                stringId = requireNotNull(StringId.of(stringId)) { "Invalid string id: $stringId" },
                midi = midi,
                timestampMs = timestampMs
            )
        )
    }

    suspend fun emitConfiguration(state: InstrumentState) {
        emitEvent(InstrumentEvent.ConfigurationUpdated(state))
    }

    suspend fun emitBatteryState(percent: Int, millivolts: Int, flags: Int) {
        emitEvent(
            InstrumentEvent.BatteryUpdated(
                percent = percent,
                millivolts = millivolts,
                flags = flags
            )
        )
    }

    suspend fun emitError(code: Int, referenceSequence: Int, referenceType: Int) {
        emitEvent(
            InstrumentEvent.ErrorReported(
                code = code,
                referenceSequence = referenceSequence,
                referenceType = referenceType
            )
        )
    }

    fun clearRecordedCommands() {
        recordedCommands.clear()
    }

    override suspend fun scanForDevices() {
        if (scanFailures.isNotEmpty()) {
            throw scanFailures.removeFirst()
        }
    }

    override suspend fun connect(device: InstrumentDevice) {
        _connectionState.value = ConnectionState.CONNECTING
        if (connectFailures.isNotEmpty()) {
            _connectionState.value = ConnectionState.DISCONNECTED
            throw connectFailures.removeFirst()
        }

        _connectedDeviceName.value = device.name
        _connectionState.value = ConnectionState.CONNECTED
    }

    override suspend fun send(command: InstrumentCommand) {
        check(_connectionState.value == ConnectionState.CONNECTED) {
            "Fake instrument is not connected"
        }
        if (commandFailures.isNotEmpty()) {
            throw commandFailures.removeFirst()
        }
        recordedCommands += command
    }

    override fun disconnect() {
        _connectionState.value = ConnectionState.DISCONNECTED
    }
}
