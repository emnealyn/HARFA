package com.example.harpapp.device.simulator

import com.example.harpapp.device.bluetooth.instrumentCapabilitiesOf
import com.example.harpapp.device.bluetooth.toBatteryStatus
import com.example.harpapp.domain.model.BatteryPowerState
import com.example.harpapp.domain.model.BatteryStatus
import com.example.harpapp.domain.model.ConnectionState
import com.example.harpapp.domain.model.InstrumentCapabilities
import com.example.harpapp.domain.model.InstrumentCommand
import com.example.harpapp.domain.model.InstrumentDevice
import com.example.harpapp.domain.model.InstrumentEvent
import com.example.harpapp.domain.model.InstrumentState
import com.example.harpapp.domain.model.NoteEvent
import com.example.harpapp.domain.model.SessionId
import com.example.harpapp.domain.model.StaleSessionException
import com.example.harpapp.domain.model.StringId
import com.example.harpapp.domain.repository.InstrumentRepository
import com.example.harpapp.protocol.Capabilities
import com.example.harpapp.protocol.HarpMessage
import com.example.harpapp.protocol.LessonResultCode
import com.example.harpapp.protocol.SessionIds
import com.example.harpapp.protocol.StateRequestFlags
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import java.util.ArrayDeque

/**
 * Symulator harfy (HARFA-25): do testów jednostkowych i trybu demo w buildzie debug.
 * Nie korzysta z Android Bluetooth API. Nie trafia do wersji release (src/debug).
 */
class FakeInstrumentRepository(
    private val config: SimulatorConfig = SimulatorConfig()
) : InstrumentRepository {

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.DISCONNECTED)
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

    private val _capabilities = MutableStateFlow(InstrumentCapabilities.NONE)
    override val capabilities: Flow<InstrumentCapabilities> = _capabilities.asStateFlow()

    // --- Stan widoczny dla panelu demo ---

    /** Maska zapalonych diod (bit i = struna i), z ostatniego LED_SET albo LESSON_STEP. */
    private val _litStrings = MutableStateFlow(0)
    val litStrings: StateFlow<Int> = _litStrings.asStateFlow()

    /** Ostatnie [COMMAND_LOG_SIZE] komend otrzymanych od aplikacji. */
    private val _commandLog = MutableStateFlow<List<InstrumentCommand>>(emptyList())
    val commandLog: StateFlow<List<InstrumentCommand>> = _commandLog.asStateFlow()

    private val _battery = MutableStateFlow(
        InstrumentEvent.BatteryUpdated(
            BatteryStatus(
                percent = config.batteryPercent,
                millivolts = config.batteryMillivolts,
                powerState = BatteryPowerState.DISCHARGING,
                isLow = false
            )
        )
    )
    val battery: StateFlow<InstrumentEvent.BatteryUpdated> = _battery.asStateFlow()

    override val batteryStatus: Flow<BatteryStatus?> = combine(
        _connectionState,
        _battery
    ) { connection, battery ->
        battery.status.takeIf {
            connection == ConnectionState.CONNECTED &&
                config.capabilities and Capabilities.BATTERY != 0
        }
    }

    val currentConnectionState: ConnectionState
        get() = _connectionState.value

    // --- Sterowanie testowe ---

    private val recordedCommands = mutableListOf<InstrumentCommand>()
    private val scanFailures = ArrayDeque<Throwable>()
    private val connectFailures = ArrayDeque<Throwable>()
    private val commandFailures = ArrayDeque<Throwable>()
    private var presetDevices: List<InstrumentDevice>? = null
    private var lastDevice: InstrumentDevice? = null
    private var timestampMs = 0
    
    private val _currentSession = MutableStateFlow<SessionId?>(null)
    override val currentSession: StateFlow<SessionId?> = _currentSession.asStateFlow()
    private var sessionCounter = 0

    private var lessonStepMask = 0
    private var lessonPaused = false

    val sentCommands: List<InstrumentCommand>
        get() = recordedCommands.toList()

    /** Nadpisuje wynik skanowania. Domyślnie skan znajduje [SimulatorConfig.device]. */
    fun setDiscoveredDevices(devices: List<InstrumentDevice>) {
        presetDevices = devices
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
        if (state == ConnectionState.CONNECTED) {
            _connectedDeviceName.value = deviceName
            _capabilities.value = instrumentCapabilitiesOf(config.capabilities)
        } else if (state == ConnectionState.DISCONNECTED) {
            clearLessonState()
            _capabilities.value = InstrumentCapabilities.NONE
        }
        updateConnectionState(state)
    }

    fun simulateConnectionLoss() {
        updateConnectionState(ConnectionState.DISCONNECTED)
        _capabilities.value = InstrumentCapabilities.NONE
        clearLessonState()
    }

    /** Ponowne połączenie z ostatnim urządzeniem, z pełnym „handshake”. */
    suspend fun reconnect() {
        connect(lastDevice ?: config.device)
    }

    /**
     * Zagranie struny; MIDI liczone z bieżącego strojenia ([InstrumentState.effectiveMidi]).
     * Bez połączenia harfa nic nie wysyła – zwraca `false`.
     */
    suspend fun pluck(stringId: Int): Boolean {
        if (_connectionState.value != ConnectionState.CONNECTED) return false
        val id = requireNotNull(StringId.of(stringId)) { "Invalid string id: $stringId" }
        timestampMs = (timestampMs + TIMESTAMP_STEP_MS) and 0xFFFF
        emitEvent(
            NoteEvent(
                stringId = id,
                midi = _instrumentState.value.effectiveMidi(id),
                timestampMs = timestampMs
            )
        )
        return true
    }

    /** Zagranie najniższej podświetlonej struny. Gdy żadna nie świeci – `false`. */
    suspend fun pluckLitString(): Boolean {
        val mask = _litStrings.value
        if (mask == 0) return false
        return pluck(Integer.numberOfTrailingZeros(mask))
    }

    /** Deterministyczny scenariusz testowy wykonywany krok po kroku. */
    suspend fun runScenario(steps: List<SimulatorStep>) {
        for (step in steps) {
            when (step) {
                SimulatorStep.Scan -> scanForDevices()
                SimulatorStep.Connect -> connect(config.device)
                SimulatorStep.Disconnect -> disconnect()
                SimulatorStep.LoseConnection -> simulateConnectionLoss()
                SimulatorStep.Reconnect -> reconnect()
                is SimulatorStep.Pluck -> pluck(step.stringId)
                SimulatorStep.PluckLit -> pluckLitString()
                is SimulatorStep.Battery ->
                    emitBatteryState(step.percent, step.millivolts, step.flags)
                is SimulatorStep.Error ->
                    emitError(step.code, step.referenceSequence, step.referenceType)
                is SimulatorStep.Wait -> delay(step.millis)
            }
        }
    }

    suspend fun emitEvent(event: InstrumentEvent) {
        _events.emit(event)
        when (event) {
            is NoteEvent -> _noteEvents.emit(event)
            is InstrumentEvent.ConfigurationUpdated -> _instrumentState.value = event.state
            is InstrumentEvent.BatteryUpdated -> _battery.value = event
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
        val status = requireNotNull(
            HarpMessage.BatteryState(
                seq = 0,
                sid = SessionIds.MIN,
                percent = percent,
                millivolts = millivolts,
                flags = flags
            ).toBatteryStatus()
        ) { "Invalid battery percent: $percent" }
        emitEvent(InstrumentEvent.BatteryUpdated(status))
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
        _commandLog.value = emptyList()
    }

    // --- InstrumentRepository ---

    override suspend fun scanForDevices() {
        if (scanFailures.isNotEmpty()) {
            throw scanFailures.removeFirst()
        }
        delay(config.scanDelayMs)
        _discoveredDevices.value = presetDevices ?: listOf(config.device)
    }

    override suspend fun connect(device: InstrumentDevice) {
        updateConnectionState(ConnectionState.CONNECTING)
        if (connectFailures.isNotEmpty()) {
            updateConnectionState(ConnectionState.DISCONNECTED)
            throw connectFailures.removeFirst()
        }
        delay(config.connectDelayMs)

        lastDevice = device
        _connectedDeviceName.value = device.name
        _connectedDeviceId.value = config.deviceId
        clearLessonState()
        _capabilities.value = instrumentCapabilitiesOf(config.capabilities)
        updateConnectionState(ConnectionState.CONNECTED)

        // Jak po handshake: CONFIG_STATE, potem BATTERY_STATE (spec §6).
        emitConfiguration(_instrumentState.value)
        if (config.capabilities and Capabilities.BATTERY != 0) {
            emitEvent(_battery.value)
        }
    }

    override suspend fun send(command: InstrumentCommand, session: SessionId) {
        check(_connectionState.value == ConnectionState.CONNECTED) {
            "Fake instrument is not connected"
        }
        if (session != _currentSession.value) {
            throw StaleSessionException(
                "Command prepared for session ${session.value}, current session is ${_currentSession.value?.value}"
            )
        }
        if (commandFailures.isNotEmpty()) {
            throw commandFailures.removeFirst()
        }
        validateCapability(command)

        recordedCommands += command
        _commandLog.value = (_commandLog.value + command).takeLast(COMMAND_LOG_SIZE)
        react(command)
    }

    override fun disconnect() {
        updateConnectionState(ConnectionState.DISCONNECTED)
        _capabilities.value = InstrumentCapabilities.NONE
        clearLessonState()
    }

    // --- Wewnętrzne ---

    private suspend fun react(command: InstrumentCommand) {
        when (command) {
            is InstrumentCommand.SetLeds -> _litStrings.value = command.mask
            is InstrumentCommand.StartLesson -> clearLessonState()
            is InstrumentCommand.LessonStep -> {
                lessonStepMask = command.mask
                if (!lessonPaused) _litStrings.value = command.mask
            }
            is InstrumentCommand.LessonResult ->
                if (command.result == LessonResultCode.COMPLETE) {
                    lessonStepMask = 0
                    _litStrings.value = 0
                }
            InstrumentCommand.EndLesson -> clearLessonState()
            is InstrumentCommand.PauseLesson -> {
                lessonPaused = command.paused
                _litStrings.value = if (command.paused) 0 else lessonStepMask
            }
            is InstrumentCommand.RequestState -> {
                if (command.flags and StateRequestFlags.CONFIG != 0) {
                    emitConfiguration(_instrumentState.value)
                }
                if (command.flags and StateRequestFlags.BATTERY != 0) {
                    emitEvent(_battery.value)
                }
            }
        }
    }

    /** Te same reguły co w BluetoothInstrumentRepository.validateCommandCapability. */
    private fun validateCapability(command: InstrumentCommand) {
        val caps = config.capabilities
        when (command) {
            is InstrumentCommand.RequestState ->
                if (command.flags and StateRequestFlags.BATTERY != 0) {
                    require(caps and Capabilities.BATTERY != 0) {
                        "Instrument does not support battery state requests"
                    }
                }

            is InstrumentCommand.SetLeds ->
                require(caps and Capabilities.LEDS != 0) {
                    "Instrument does not support LED commands"
                }

            is InstrumentCommand.StartLesson,
            is InstrumentCommand.LessonStep,
            is InstrumentCommand.LessonResult,
            is InstrumentCommand.PauseLesson,
            InstrumentCommand.EndLesson ->
                require(caps and Capabilities.LESSON != 0) {
                    "Instrument does not support lesson commands"
                }
        }
    }

    private fun updateConnectionState(state: ConnectionState) {
        if (state != ConnectionState.CONNECTED) {
            _currentSession.value = null
        } else if (_connectionState.value != ConnectionState.CONNECTED || _currentSession.value == null) {
            _currentSession.value = SessionId(++sessionCounter)
        }
        _connectionState.value = state
    }

    private fun clearLessonState() {
        lessonStepMask = 0
        lessonPaused = false
        _litStrings.value = 0
    }

    private companion object {
        const val COMMAND_LOG_SIZE = 20
        const val TIMESTAMP_STEP_MS = 250
    }
}
