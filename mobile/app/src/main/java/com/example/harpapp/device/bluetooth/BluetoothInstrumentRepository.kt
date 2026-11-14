package com.example.harpapp.device.bluetooth

import android.content.Context
import android.util.Log
import com.example.harpapp.domain.model.BatteryStatus
import com.example.harpapp.domain.model.ConnectionError
import com.example.harpapp.domain.model.ConnectionErrorCode
import com.example.harpapp.domain.model.ConnectionState
import com.example.harpapp.domain.model.ConnectionStateMachine
import com.example.harpapp.domain.model.InstrumentCapabilities
import com.example.harpapp.domain.model.InstrumentCommand
import com.example.harpapp.domain.model.InstrumentDevice
import com.example.harpapp.domain.model.InstrumentEvent
import com.example.harpapp.domain.model.InstrumentState
import com.example.harpapp.domain.model.NoteEvent
import com.example.harpapp.domain.model.SessionId
import com.example.harpapp.domain.model.StaleSessionException
import com.example.harpapp.domain.repository.InstrumentRepository
import com.example.harpapp.domain.model.isOperationInProgress
import com.example.harpapp.protocol.HarpMessage
import com.example.harpapp.protocol.HarpProtocol
import com.example.harpapp.protocol.LessonPauseCode
import com.example.harpapp.protocol.StateRequestFlags
import com.example.harpapp.protocol.Capabilities
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference

class BluetoothInstrumentRepository internal constructor(
    context: Context,
    private val transport: InstrumentTransport = BleGattInstrumentTransport(context),
    private val scanner: BleInstrumentScanner = BleInstrumentScanner(context)
) : InstrumentRepository {

    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val session = HarpSession { frame -> transport.send(frame) }
    private val stateMachine = ConnectionStateMachine()
    private val activeOperation = AtomicReference<Job?>(null)
    private val eventStream = InstrumentEventStream()
    private val deviceCapabilities = MutableStateFlow<Int?>(null)
    override val capabilities: Flow<InstrumentCapabilities> =
        deviceCapabilities.map { it?.let(::instrumentCapabilitiesOf) ?: InstrumentCapabilities.NONE }
    private val _currentSession = MutableStateFlow<SessionId?>(null)
    override val currentSession: StateFlow<SessionId?> = _currentSession.asStateFlow()
    private var stateRecoveryRequested = false
    private var stateRecoveryJob: Job? = null

    private val _connectionState = MutableStateFlow(stateMachine.state)
    override val connectionState: Flow<ConnectionState> = _connectionState.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow(
        preferences.getString(LAST_DEVICE_NAME_KEY, null)
    )
    override val connectedDeviceName: Flow<String?> = _connectedDeviceName.asStateFlow()

    private val _connectedDeviceId = MutableStateFlow<Long?>(
        if (preferences.contains(LAST_DEVICE_ID_KEY)) {
            preferences.getLong(LAST_DEVICE_ID_KEY, 0L)
        } else {
            null
        }
    )
    override val connectedDeviceId: Flow<Long?> = _connectedDeviceId.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<InstrumentDevice>>(emptyList())
    override val discoveredDevices: Flow<List<InstrumentDevice>> =
        _discoveredDevices.asStateFlow()

    private val _events = MutableSharedFlow<InstrumentEvent>(extraBufferCapacity = 16)
    override val events: Flow<InstrumentEvent> = _events

    override val noteEvents: Flow<NoteEvent> = eventStream.noteEvents
    override val instrumentState: Flow<InstrumentState> = eventStream.instrumentState

    override val batteryStatus: Flow<BatteryStatus?> = combine(
        _connectionState,
        deviceCapabilities,
        eventStream.batteryStatus
    ) { connection, capabilities, status ->
        status.takeIf {
            connection == ConnectionState.CONNECTED &&
                    capabilities != null &&
                    capabilities and Capabilities.BATTERY != 0
        }
    }

    init {
        ioScope.launch {
            transport.events.collect { event ->
                when (event) {
                    is TransportEvent.Data -> handleFrame(event.bytes)
                    TransportEvent.Disconnected -> handleTransportDisconnected()
                }
            }
        }
    }

    override suspend fun scanForDevices() {
        awaitPreviousOperation("scanning")
        runExclusive("scanning") {
            stateMachine.startScanning()
            publishState()

            try {
                _discoveredDevices.value = scanner.scan()
                transitionTo(ConnectionState.DISCONNECTED)
            } catch (exception: CancellationException) {
                transitionTo(ConnectionState.DISCONNECTED)
                throw exception
            } catch (exception: SecurityException) {
                transitionTo(ConnectionState.PERMISSION_REQUIRED)
                throw exception
            } catch (exception: BluetoothUnavailableException) {
                transitionTo(ConnectionState.BLUETOOTH_DISABLED)
                throw exception
            } catch (exception: Exception) {
                Log.e(TAG, "BLE instrument scan failed", exception)
                fail(
                    ConnectionError(
                        code = ConnectionErrorCode.SCAN_FAILED,
                        detail = exception.message
                    ),
                    exception
                )
                throw exception
            }
        }
    }

    override suspend fun connect(device: InstrumentDevice) {
        if (stateMachine.state == ConnectionState.CONNECTED) {
            disconnect()
        }

        awaitPreviousOperation("connecting", cancelScanning = true)
        runExclusive("connecting") {
            stateMachine.startConnecting()
            publishState()

            try {
                transport.connect(device, ::handleTransportStage)
                transitionTo(ConnectionState.HANDSHAKING)

                val deviceInfo = session.start()
                validateDeviceInfo(deviceInfo)
                deviceCapabilities.value = deviceInfo.caps
                _currentSession.value = SessionId(session.currentGeneration)

                transitionTo(ConnectionState.CONNECTED)
                val nameToSave = device.name ?: device.address
                _connectedDeviceName.value = nameToSave
                _connectedDeviceId.value = deviceInfo.deviceId
                preferences.edit()
                    .putString(LAST_DEVICE_NAME_KEY, nameToSave)
                    .putLong(LAST_DEVICE_ID_KEY, deviceInfo.deviceId)
                    .apply()
                requestStateRecoveryIfNeeded()
            } catch (exception: CancellationException) {
                clearConnectionData()
                transitionTo(ConnectionState.DISCONNECTED)
                throw exception
            } catch (exception: SecurityException) {
                fail(
                    ConnectionState.PERMISSION_REQUIRED,
                    "Bluetooth permissions are missing",
                    exception
                )
            } catch (exception: BluetoothUnavailableException) {
                fail(
                    ConnectionState.BLUETOOTH_DISABLED,
                    "Bluetooth adapter or GATT is unavailable",
                    exception
                )
            } catch (exception: BleConnectionTimeoutException) {
                fail(
                    ConnectionErrorCode.TIMEOUT,
                    "BLE GATT connection timed out",
                    exception
                )
            } catch (exception: BleServiceDiscoveryException) {
                fail(
                    ConnectionErrorCode.SERVICE_DISCOVERY_FAILED,
                    "BLE GATT service discovery failed",
                    exception
                )
            } catch (exception: HarpHandshakeException) {
                fail(
                    ConnectionErrorCode.HANDSHAKE_FAILED,
                    "BLE handshake failed",
                    exception
                )
            } catch (exception: IllegalArgumentException) {
                fail(
                    ConnectionErrorCode.CONNECTION_FAILED,
                    "Invalid BLE instrument address: ${device.address}",
                    exception
                )
            } catch (exception: IOException) {
                fail(
                    ConnectionErrorCode.CONNECTION_FAILED,
                    "BLE instrument connection failed",
                    exception
                )
            } catch (exception: Exception) {
                fail(
                    ConnectionErrorCode.UNKNOWN,
                    "Unexpected BLE connection failure",
                    exception
                )
            }
        }
    }

    override suspend fun send(command: InstrumentCommand, session: SessionId) {
        withContext(Dispatchers.IO) {
            try {
                val capabilities = deviceCapabilities.value
                    ?: throw IllegalStateException("HARFA instrument is not connected")
                val currentSession = _currentSession.value
                if (currentSession != session) {
                    throw StaleSessionException(
                        "Command prepared for session ${session.value}, " +
                            "current session is ${currentSession?.value}"
                    )
                }
                validateCommandCapability(command, capabilities)
                val wireSid = this@BluetoothInstrumentRepository.session.currentSid
                val generation = this@BluetoothInstrumentRepository.session.currentGeneration
                this@BluetoothInstrumentRepository.session.sendCommand(
                    expectedSid = wireSid,
                    expectedGeneration = generation
                ) { sequence, sid -> command.toHarpMessage(sequence, sid) }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: HarpStaleSessionException) {
                throw StaleSessionException(exception.message ?: "Command from a finished session", exception)
            } catch (exception: HarpCommandTimeoutException) {
                fail(
                    ConnectionErrorCode.TIMEOUT,
                    "No ACK received from the BLE instrument",
                    exception
                )
                throw exception
            } catch (exception: SecurityException) {
                fail(
                    ConnectionState.PERMISSION_REQUIRED,
                    "Bluetooth permissions are missing",
                    exception
                )
                throw exception
            } catch (exception: HarpCommandException) {
                throw exception
            } catch (exception: IOException) {
                fail(
                    ConnectionErrorCode.CONNECTION_FAILED,
                    "Could not send command to the BLE instrument",
                    exception
                )
                throw exception
            }
        }
    }

    override fun disconnect() {
        val currentState = stateMachine.state
        if (
            currentState == ConnectionState.CONNECTED ||
            (currentState.isOperationInProgress && currentState != ConnectionState.DISCONNECTING)
        ) {
            transitionTo(ConnectionState.DISCONNECTING)
        }

        activeOperation.get()?.cancel()
        clearConnectionData()
        transport.disconnect()
        transitionTo(ConnectionState.DISCONNECTED)
    }

    private suspend fun handleFrame(frame: ByteArray) {
        when (val event = session.handleIncoming(frame)) {
            is HarpSessionEvent.InvalidFrame -> {
                Log.e(TAG, "Ignoring invalid HARFA frame: ${event.status}")
            }

            is HarpSessionEvent.StaleFrame -> {
                Log.w(
                    TAG,
                    "Ignoring HARFA ${event.message.type} from session ${event.message.sid}, " +
                        "current session is ${event.currentSid}"
                )
            }

            is HarpSessionEvent.Message -> {
                event.sequenceGap?.let { gap ->
                    Log.w(
                        TAG,
                        "HARFA sequence gap detected: expected=${gap.expectedSequence}, " +
                            "actual=${gap.actualSequence}"
                    )
                    stateRecoveryRequested = true
                    requestStateRecoveryIfNeeded()
                }

                when (val message = event.message) {
                    // Zagrania, strojenie i bateria trafiają do modelu domenowego (docs/domain-model.md §7).
                    is HarpMessage.StringEvent,
                    is HarpMessage.ConfigState,
                    is HarpMessage.BatteryState -> {
                        val domainEvent = eventStream.accept(message)
                        if (domainEvent != null) {
                            _events.emit(domainEvent)
                        } else {
                            Log.w(TAG, "Ignoring HARFA message with invalid values: $message")
                        }
                    }

                    is HarpMessage.Error -> _events.emit(
                        InstrumentEvent.ErrorReported(
                            code = message.code,
                            referenceSequence = message.refSeq,
                            referenceType = message.refType
                        )
                    )

                    else -> Unit
                }
            }
        }
    }

    private fun validateCommandCapability(
        command: InstrumentCommand,
        capabilities: Int
    ) {
        when (command) {
            is InstrumentCommand.RequestState -> {
                require(command.flags and StateRequestFlags.KNOWN_MASK == command.flags) {
                    "Unknown state request flags: ${command.flags}"
                }
                if (command.flags and StateRequestFlags.BATTERY != 0) {
                    require(capabilities and Capabilities.BATTERY != 0) {
                        "Instrument does not support battery state requests"
                    }
                }
            }

            is InstrumentCommand.SetLeds -> {
                require(capabilities and Capabilities.LEDS != 0) {
                    "Instrument does not support LED commands"
                }
            }

            is InstrumentCommand.StartLesson,
            is InstrumentCommand.LessonStep,
            is InstrumentCommand.LessonResult,
            is InstrumentCommand.PauseLesson,
            InstrumentCommand.EndLesson -> {
                require(capabilities and Capabilities.LESSON != 0) {
                    "Instrument does not support lesson commands"
                }
            }
        }
    }

    private fun InstrumentCommand.toHarpMessage(sequence: Int, sid: Int): HarpMessage {
        return when (this) {
            is InstrumentCommand.RequestState ->
                HarpMessage.StateRequest(sequence, sid, flags)
            is InstrumentCommand.SetLeds ->
                HarpMessage.LedSet(sequence, sid, mask, mode)
            is InstrumentCommand.StartLesson ->
                HarpMessage.LessonStart(sequence, sid, totalSteps)
            is InstrumentCommand.LessonStep ->
                HarpMessage.LessonStep(sequence, sid, index, mask)
            is InstrumentCommand.LessonResult ->
                HarpMessage.LessonResult(sequence, sid, result)
            InstrumentCommand.EndLesson ->
                HarpMessage.LessonEnd(sequence, sid)
            is InstrumentCommand.PauseLesson ->
                HarpMessage.LessonPause(
                    sequence,
                    sid,
                    if (paused) LessonPauseCode.PAUSE else LessonPauseCode.RESUME
                )
        }
    }

    private fun validateDeviceInfo(deviceInfo: HarpMessage.DeviceInfo) {
        if (deviceInfo.protoMajor != HarpProtocol.VERSION_MAJOR) {
            throw HarpHandshakeException(
                "Unsupported HARFA protocol major version: ${deviceInfo.protoMajor}"
            )
        }
        if (deviceInfo.protoMinor > HarpProtocol.VERSION_MINOR) {
            throw HarpHandshakeException(
                "Unsupported HARFA protocol minor version: ${deviceInfo.protoMinor}"
            )
        }
        if (deviceInfo.stringCount != HarpProtocol.STRING_COUNT) {
            throw HarpHandshakeException(
                "Unsupported instrument string count: ${deviceInfo.stringCount}"
            )
        }
    }

    private fun requestStateRecoveryIfNeeded() {
        if (
            !stateRecoveryRequested ||
            _connectionState.value != ConnectionState.CONNECTED ||
            stateRecoveryJob?.isActive == true
        ) {
            return
        }

        stateRecoveryJob = ioScope.launch {
            while (stateRecoveryRequested && _connectionState.value == ConnectionState.CONNECTED) {
                stateRecoveryRequested = false
                val flags = StateRequestFlags.CONFIG or
                    if (deviceCapabilities.value?.and(Capabilities.BATTERY) != 0) {
                        StateRequestFlags.BATTERY
                    } else {
                        0
                    }

                try {
                    send(InstrumentCommand.RequestState(flags), _currentSession.value ?: break)
                } catch (exception: CancellationException) {
                    if (!currentCoroutineContext().isActive) {
                        throw exception
                    }
                    Log.w(TAG, "State recovery request was interrupted", exception)
                } catch (exception: Exception) {
                    Log.e(TAG, "Could not request state after a sequence gap", exception)
                    break
                }
            }

            stateRecoveryJob = null
            requestStateRecoveryIfNeeded()
        }
    }

    private suspend fun <T> runExclusive(
        operation: String,
        block: suspend () -> T
    ): T {
        val operationJob = requireNotNull(currentCoroutineContext()[Job]) {
            "BLE operations must run in a coroutine"
        }
        check(activeOperation.compareAndSet(null, operationJob)) {
            "Cannot start $operation while another BLE operation is active"
        }

        try {
            return block()
        } finally {
            activeOperation.compareAndSet(operationJob, null)
        }
    }

    private suspend fun awaitPreviousOperation(
        operation: String,
        cancelScanning: Boolean = false
    ) {
        val currentJob = currentCoroutineContext()[Job] ?: return
        val previousJob = activeOperation.get() ?: return
        if (previousJob === currentJob) return

        when {
            cancelScanning && stateMachine.state == ConnectionState.SCANNING -> {
                previousJob.cancel(
                    CancellationException("BLE scan cancelled before $operation")
                )
                previousJob.join()
            }

            stateMachine.state == ConnectionState.DISCONNECTED ||
                stateMachine.state is ConnectionState.ERROR -> {
                previousJob.join()
            }
        }
    }

    private fun handleTransportStage(stage: TransportConnectionStage) {
        when (stage) {
            TransportConnectionStage.CONNECTING -> Unit
            TransportConnectionStage.DISCOVERING_SERVICES ->
                if (stateMachine.state == ConnectionState.CONNECTING) {
                    transitionTo(ConnectionState.DISCOVERING_SERVICES)
                }

            TransportConnectionStage.READY_FOR_HANDSHAKE -> Unit
        }
    }

    private fun handleTransportDisconnected() {
        val stateBeforeDisconnect = stateMachine.state
        clearConnectionData()

        if (
            stateBeforeDisconnect !is ConnectionState.ERROR &&
            stateBeforeDisconnect != ConnectionState.BLUETOOTH_DISABLED &&
            stateBeforeDisconnect != ConnectionState.PERMISSION_REQUIRED
        ) {
            transitionTo(ConnectionState.DISCONNECTED)
        }
    }

    private fun clearConnectionData() {
        stateRecoveryRequested = false
        stateRecoveryJob?.cancel()
        stateRecoveryJob = null
        session.reset()
        _currentSession.value = null
        eventStream.reset()
        deviceCapabilities.value = null
    }

    private fun fail(
        error: ConnectionError,
        exception: Throwable
    ) {
        Log.e(TAG, error.detail ?: "BLE operation failed", exception)
        clearConnectionData()
        transport.disconnect()
        stateMachine.fail(error)
        publishState()
    }

    private fun fail(
        code: ConnectionErrorCode,
        message: String,
        exception: Throwable
    ) {
        fail(
            ConnectionError(
                code = code,
                detail = exception.message ?: message
            ),
            exception
        )
    }

    private fun fail(
        state: ConnectionState,
        message: String,
        exception: Throwable
    ) {
        Log.e(TAG, message, exception)
        clearConnectionData()
        transport.disconnect()
        transitionTo(state)
    }

    private fun transitionTo(next: ConnectionState) {
        stateMachine.transitionTo(next)
        publishState()
    }

    private fun publishState() {
        _connectionState.value = stateMachine.state
    }

    private companion object {
        const val TAG = "BluetoothInstrumentRepository"
        const val PREFERENCES_NAME = "bluetooth_prefs"
        const val LAST_DEVICE_NAME_KEY = "last_device_name"
        const val LAST_DEVICE_ID_KEY = "last_device_id"
    }
}
