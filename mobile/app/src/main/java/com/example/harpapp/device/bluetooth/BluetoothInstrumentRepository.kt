package com.example.harpapp.device.bluetooth

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.example.harpapp.domain.model.ConnectionState
import com.example.harpapp.domain.model.InstrumentCommand
import com.example.harpapp.domain.model.InstrumentDevice
import com.example.harpapp.domain.model.InstrumentEvent
import com.example.harpapp.domain.model.NotePlayed
import com.example.harpapp.domain.repository.InstrumentRepository
import com.example.harpapp.protocol.HarpMessage
import com.example.harpapp.protocol.HarpProtocol
import com.example.harpapp.protocol.StateRequestFlags
import com.example.harpapp.protocol.Capabilities
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.IOException

@SuppressLint("MissingPermission")
class BluetoothInstrumentRepository internal constructor(
    context: Context,
    private val transport: InstrumentTransport = BleGattInstrumentTransport(context),
    private val scanner: BleInstrumentScanner = BleInstrumentScanner(context)
) : InstrumentRepository {

    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val session = HarpSession { frame -> transport.send(frame) }
    private var deviceCapabilities: Int? = null

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: Flow<ConnectionState> = _connectionState.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow(
        preferences.getString(LAST_DEVICE_NAME_KEY, null)
    )
    override val connectedDeviceName: Flow<String?> = _connectedDeviceName.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<InstrumentDevice>>(emptyList())
    override val discoveredDevices: Flow<List<InstrumentDevice>> =
        _discoveredDevices.asStateFlow()

    private val _events = MutableSharedFlow<InstrumentEvent>(extraBufferCapacity = 16)
    override val events: Flow<InstrumentEvent> = _events

    private val _incomingNotes = MutableSharedFlow<NotePlayed>(extraBufferCapacity = 16)
    override val incomingNotes: Flow<NotePlayed> = _incomingNotes

    init {
        ioScope.launch {
            transport.events.collect { event ->
                when (event) {
                    is TransportEvent.Data -> handleFrame(event.bytes)
                    TransportEvent.Disconnected -> {
                        session.reset()
                        deviceCapabilities = null
                        _connectionState.value = ConnectionState.DISCONNECTED
                    }
                }
            }
        }
    }

    override suspend fun scanForDevices() {
        withContext(Dispatchers.IO) {
            try {
                _discoveredDevices.value = scanner.scan()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.e(TAG, "BLE instrument scan failed", exception)
                throw exception
            }
        }
    }

    override suspend fun connect(device: InstrumentDevice) {
        withContext(Dispatchers.IO) {
            disconnect()
            _connectionState.value = ConnectionState.CONNECTING

            try {
                transport.connect(device)
                val deviceInfo = session.start()
                validateDeviceInfo(deviceInfo)
                deviceCapabilities = deviceInfo.caps

                _connectionState.value = ConnectionState.CONNECTED
                val nameToSave = device.name ?: device.address
                _connectedDeviceName.value = nameToSave
                preferences.edit().putString(LAST_DEVICE_NAME_KEY, nameToSave).apply()
            } catch (exception: CancellationException) {
                if (!currentCoroutineContext().isActive) {
                    throw exception
                }
                handleConnectionFailure("BLE handshake was cancelled", exception)
            } catch (exception: IllegalArgumentException) {
                handleConnectionFailure("Invalid BLE instrument address: ${device.address}", exception)
            } catch (exception: IllegalStateException) {
                handleConnectionFailure("BLE adapter or GATT is unavailable", exception)
            } catch (exception: IOException) {
                handleConnectionFailure("BLE instrument connection failed", exception)
            } catch (exception: SecurityException) {
                handleConnectionFailure("Bluetooth permissions are missing", exception)
            }
        }
    }

    override suspend fun send(command: InstrumentCommand) {
        withContext(Dispatchers.IO) {
            try {
                val capabilities = deviceCapabilities
                    ?: throw IllegalStateException("HARFA instrument is not connected")
                validateCommandCapability(command, capabilities)
                session.sendCommand { sequence -> command.toHarpMessage(sequence) }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: HarpCommandTimeoutException) {
                handleConnectionFailure("No ACK received from the BLE instrument", exception)
                throw exception
            } catch (exception: HarpCommandException) {
                throw exception
            } catch (exception: IOException) {
                handleConnectionFailure("Could not send command to the BLE instrument", exception)
                throw exception
            }
        }
    }

    override fun disconnect() {
        session.reset()
        deviceCapabilities = null
        transport.disconnect()
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    private suspend fun handleFrame(frame: ByteArray) {
        when (val event = session.handleIncoming(frame)) {
            is HarpSessionEvent.InvalidFrame -> {
                Log.e(TAG, "Ignoring invalid HARFA frame: ${event.status}")
            }

            is HarpSessionEvent.Message -> {
                when (val message = event.message) {
                    is HarpMessage.StringEvent -> {
                        val note = NotePlayed(
                            stringId = message.stringId,
                            midi = message.midi,
                            timestampMs = message.tMs
                        )
                        _events.emit(note)
                        _incomingNotes.emit(note)
                    }

                    is HarpMessage.BatteryState -> _events.emit(
                        InstrumentEvent.BatteryUpdated(
                            percent = message.percent,
                            millivolts = message.millivolts,
                            flags = message.flags
                        )
                    )

                    is HarpMessage.ConfigState -> _events.emit(
                        InstrumentEvent.ConfigurationUpdated(
                            octaveShift = message.octaveShift,
                            lever = message.lever,
                            baseOctave = message.baseOctave
                        )
                    )

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
            }

            is InstrumentCommand.SetLeds -> {
                require(capabilities and Capabilities.LEDS != 0) {
                    "Instrument does not support LED commands"
                }
            }

            is InstrumentCommand.StartLesson,
            is InstrumentCommand.LessonStep,
            is InstrumentCommand.LessonResult,
            InstrumentCommand.EndLesson -> {
                require(capabilities and Capabilities.LESSON != 0) {
                    "Instrument does not support lesson commands"
                }
            }
        }
    }

    private fun InstrumentCommand.toHarpMessage(sequence: Int): HarpMessage {
        return when (this) {
            is InstrumentCommand.RequestState ->
                HarpMessage.StateRequest(sequence, flags)
            is InstrumentCommand.SetLeds ->
                HarpMessage.LedSet(sequence, mask, mode)
            is InstrumentCommand.StartLesson ->
                HarpMessage.LessonStart(sequence, totalSteps)
            is InstrumentCommand.LessonStep ->
                HarpMessage.LessonStep(sequence, index, mask)
            is InstrumentCommand.LessonResult ->
                HarpMessage.LessonResult(sequence, result)
            InstrumentCommand.EndLesson ->
                HarpMessage.LessonEnd(sequence)
        }
    }

    private fun validateDeviceInfo(deviceInfo: HarpMessage.DeviceInfo) {
        if (deviceInfo.protoMajor != HarpProtocol.VERSION_MAJOR) {
            throw HarpHandshakeException(
                "Unsupported HARFA protocol major version: ${deviceInfo.protoMajor}"
            )
        }
        if (deviceInfo.stringCount != HarpProtocol.STRING_COUNT) {
            throw HarpHandshakeException(
                "Unsupported instrument string count: ${deviceInfo.stringCount}"
            )
        }
    }

    private fun handleConnectionFailure(message: String, exception: Throwable) {
        Log.e(TAG, message, exception)
        session.reset()
        deviceCapabilities = null
        transport.disconnect()
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    private companion object {
        const val TAG = "BluetoothInstrumentRepository"
        const val PREFERENCES_NAME = "bluetooth_prefs"
        const val LAST_DEVICE_NAME_KEY = "last_device_name"
    }
}
