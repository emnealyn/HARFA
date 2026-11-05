package com.example.harpapp.device.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import com.example.harpapp.domain.model.ConnectionState
import com.example.harpapp.domain.model.NotePlayed
import com.example.harpapp.domain.model.PairedDevice
import com.example.harpapp.domain.repository.InstrumentRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("MissingPermission")
class BluetoothInstrumentRepository internal constructor(
    context: Context,
    private val codec: NoteEventCodec = NoteEventCodec(),
    private val transport: InstrumentTransport = RfcommInstrumentTransport(context),
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

    private val _incomingNotes = MutableSharedFlow<NotePlayed>(extraBufferCapacity = 16)
    override val incomingNotes: Flow<NotePlayed> = _incomingNotes

    init {
        ioScope.launch {
            transport.events.collect { event ->
                when (event) {
                    is TransportEvent.Data -> {
                        codec.decode(event.bytes, event.bytes.size).forEach { note ->
                            _incomingNotes.emit(note)
                        }
                    }

                    TransportEvent.Disconnected -> {
                        _connectionState.value = ConnectionState.DISCONNECTED
                    }
                }
            }
        }
    }

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

            try {
                transport.connect(device)
                _connectionState.value = ConnectionState.CONNECTED

                val nameToSave = device.name ?: device.address
                _connectedDeviceName.value = nameToSave
                preferences.edit().putString(LAST_DEVICE_NAME_KEY, nameToSave).apply()
            } catch (exception: IllegalArgumentException) {
                Log.e(TAG, "Invalid Bluetooth address: ${device.address}", exception)
                transport.disconnect()
                _connectionState.value = ConnectionState.DISCONNECTED
            } catch (exception: IllegalStateException) {
                Log.e(TAG, "Bluetooth adapter is unavailable", exception)
                transport.disconnect()
                _connectionState.value = ConnectionState.DISCONNECTED
            } catch (exception: java.io.IOException) {
                Log.e(TAG, "Bluetooth connection failed", exception)
                transport.disconnect()
                _connectionState.value = ConnectionState.DISCONNECTED
            } catch (exception: SecurityException) {
                Log.e(TAG, "Bluetooth permissions are missing", exception)
                transport.disconnect()
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        }
    }

    override fun disconnect() {
        transport.disconnect()
        _connectionState.value = ConnectionState.DISCONNECTED
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
        val DEFAULT_TARGET_MAC_ADDRESSES = setOf("20:E7:C8:5A:67:4E")
    }
}
