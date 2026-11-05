package com.example.harpapp.device.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import com.example.harpapp.domain.model.ConnectionState
import com.example.harpapp.domain.model.InstrumentState
import com.example.harpapp.domain.model.NoteEvent
import com.example.harpapp.domain.model.PairedDevice
import com.example.harpapp.domain.repository.InstrumentRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

@SuppressLint("MissingPermission")
class BluetoothInstrumentRepository(
    context: Context,
    private val eventStream: InstrumentEventStream = InstrumentEventStream(),
    private val targetMacAddresses: Set<String> = DEFAULT_TARGET_MAC_ADDRESSES
) : InstrumentRepository {

    private val appContext = context.applicationContext
    private val bluetoothManager = appContext.getSystemService(BluetoothManager::class.java)
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: Flow<ConnectionState> = _connectionState.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow(
        preferences.getString(LAST_DEVICE_NAME_KEY, null)
    )
    override val connectedDeviceName: Flow<String?> = _connectedDeviceName.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<PairedDevice>>(emptyList())
    override val pairedDevices: Flow<List<PairedDevice>> = _pairedDevices.asStateFlow()

    override val noteEvents: Flow<NoteEvent> = eventStream.noteEvents
    override val instrumentState: Flow<InstrumentState> = eventStream.instrumentState

    private var bluetoothSocket: BluetoothSocket? = null
    private var listeningJob: Job? = null

    override suspend fun refreshPairedDevices() {
        withContext(Dispatchers.IO) {
            val bluetoothAdapter = bluetoothManager?.adapter
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                _pairedDevices.value = emptyList()
                return@withContext
            }

            try {
                _pairedDevices.value = bluetoothAdapter.bondedDevices
                    .filter { targetMacAddresses.contains(it.address) }
                    .map { it.toPairedDevice() }
            } catch (exception: SecurityException) {
                Log.e(TAG, "Bluetooth permissions are missing", exception)
                _pairedDevices.value = emptyList()
            }
        }
    }

    override suspend fun connect(device: PairedDevice) {
        withContext(Dispatchers.IO) {
            disconnect()
            _connectionState.value = ConnectionState.CONNECTING

            val bluetoothAdapter = bluetoothManager?.adapter
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                _connectionState.value = ConnectionState.DISCONNECTED
                return@withContext
            }

            val bluetoothDevice = try {
                bluetoothAdapter.getRemoteDevice(device.address)
            } catch (exception: IllegalArgumentException) {
                Log.e(TAG, "Invalid Bluetooth address: ${device.address}", exception)
                _connectionState.value = ConnectionState.DISCONNECTED
                return@withContext
            } catch (exception: SecurityException) {
                Log.e(TAG, "Bluetooth permissions are missing", exception)
                _connectionState.value = ConnectionState.DISCONNECTED
                return@withContext
            }

            val socket = try {
                bluetoothAdapter.cancelDiscovery()
                bluetoothDevice.createRfcommSocketToServiceRecord(SPP_UUID)
            } catch (exception: SecurityException) {
                Log.e(TAG, "Bluetooth permissions are missing", exception)
                _connectionState.value = ConnectionState.DISCONNECTED
                return@withContext
            }

            try {
                socket.connect()
                bluetoothSocket = socket
                _connectionState.value = ConnectionState.CONNECTED

                val nameToSave = device.name ?: device.address
                _connectedDeviceName.value = nameToSave
                preferences.edit().putString(LAST_DEVICE_NAME_KEY, nameToSave).apply()

                startListening(socket)
            } catch (exception: IOException) {
                Log.e(TAG, "Bluetooth connection failed", exception)
                closeSocket(socket)
                _connectionState.value = ConnectionState.DISCONNECTED
            } catch (exception: SecurityException) {
                Log.e(TAG, "Bluetooth permissions are missing", exception)
                closeSocket(socket)
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        }
    }

    override fun disconnect() {
        listeningJob?.cancel()
        listeningJob = null

        val socket = bluetoothSocket
        bluetoothSocket = null
        closeSocket(socket)
        eventStream.reset()
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    private fun startListening(socket: BluetoothSocket) {
        listeningJob?.cancel()
        listeningJob = ioScope.launch {
            try {
                val inputStream = socket.inputStream
                val buffer = ByteArray(BUFFER_SIZE)

                while (socket == bluetoothSocket) {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead <= 0) {
                        break
                    }

                    // Prototypowy transport SPP nie przesyła ramek protokołu (ADR 007, ADR 009):
                    // dane są tylko logowane. Docelowy klient BLE przekaże każdą notyfikację TX
                    // jako jedną ramkę do eventStream.accept(frame).
                    Log.d(TAG, "Ignoring $bytesRead bytes from the prototype SPP transport")
                }
            } catch (exception: IOException) {
                Log.e(TAG, "Disconnected or error reading Bluetooth stream", exception)
            } catch (exception: SecurityException) {
                Log.e(TAG, "Bluetooth permissions are missing", exception)
            } finally {
                if (socket == bluetoothSocket) {
                    bluetoothSocket = null
                    eventStream.reset()
                    _connectionState.value = ConnectionState.DISCONNECTED
                }
            }
        }
    }

    private fun closeSocket(socket: BluetoothSocket?) {
        try {
            socket?.close()
        } catch (exception: IOException) {
            Log.e(TAG, "Could not close Bluetooth socket", exception)
        }
    }

    private fun BluetoothDevice.toPairedDevice(): PairedDevice {
        val deviceName = try {
            name
        } catch (exception: SecurityException) {
            Log.w(TAG, "Could not read Bluetooth device name", exception)
            null
        }

        return PairedDevice(
            name = deviceName,
            address = address
        )
    }

    private companion object {
        const val TAG = "BluetoothInstrumentRepository"
        const val PREFERENCES_NAME = "bluetooth_prefs"
        const val LAST_DEVICE_NAME_KEY = "last_device_name"
        const val BUFFER_SIZE = 1024
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        val DEFAULT_TARGET_MAC_ADDRESSES = setOf("20:E7:C8:5A:67:4E")
    }
}
