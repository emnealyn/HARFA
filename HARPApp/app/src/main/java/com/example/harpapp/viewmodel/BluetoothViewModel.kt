package com.example.harpapp.viewmodel

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID
import android.util.Log

@SuppressLint("MissingPermission")
class BluetoothViewModel(application: Application) : AndroidViewModel(application) {

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    private val prefs = application.getSharedPreferences("bluetooth_prefs", Context.MODE_PRIVATE)

    private val _pairedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDevice>> = _pairedDevices.asStateFlow()

    private var bluetoothSocket: BluetoothSocket? = null
    private var listeningJob: Job? = null

    private val _incomingNote = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val incomingNote: SharedFlow<String> = _incomingNote.asSharedFlow()

    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val TARGET_MAC_ADDRESSES = listOf(
        "20:E7:C8:5A:67:4E"
    )

    private val VALID_NOTES = setOf("C", "D", "E", "F", "G", "A", "B")

    init {
        val lastDevice = prefs.getString("last_device_name", null)
        if (lastDevice != null) {
            _connectedDeviceName.value = lastDevice
        }
    }

    fun fetchPairedDevices(context: Context) {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter?.isEnabled == true) {
            _pairedDevices.value = bluetoothAdapter.bondedDevices
                .filter { it.address in TARGET_MAC_ADDRESSES }
                .toList()
        } else {
            _pairedDevices.value = emptyList()
        }
    }

    fun connectToDevice(device: BluetoothDevice) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                disconnect()

                Log.d("BluetoothViewModel", "Attempting connection to ${device.name ?: device.address}")

                val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                bluetoothSocket = socket
                socket.connect()

                val nameToSave = device.name ?: device.address
                _isConnected.value = true
                _connectedDeviceName.value = nameToSave
                prefs.edit().putString("last_device_name", nameToSave).apply()

                Log.d("BluetoothViewModel", "Connected successfully to $nameToSave")

                startListening(socket)
            } catch (e: IOException) {
                Log.e("BluetoothViewModel", "Connection failed", e)
                e.printStackTrace()
                _isConnected.value = false
                val lastDevice = prefs.getString("last_device_name", null)
                _connectedDeviceName.value = lastDevice
            }
        }
    }

    private fun startListening(socket: BluetoothSocket) {
        listeningJob?.cancel()
        listeningJob = viewModelScope.launch(Dispatchers.IO) {
            val inputStream = try {
                socket.inputStream
            } catch (e: IOException) {
                Log.e("BluetoothViewModel", "Failed to get input stream", e)
                return@launch
            }

            Log.d("BluetoothViewModel", "Started listening for incoming data...")
            
            // Increased buffer to read potential chunks (including newlines from ESP)
            val buffer = ByteArray(1024)
            while (isActive) {
                try {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead > 0) {
                        val received = String(buffer, 0, bytesRead)
                        Log.d("BluetoothViewModel", "Received raw bytes: $bytesRead, content: '$received'")
                        
                        val trimmed = received.trim().uppercase()
                        trimmed.forEach { char ->
                            val note = char.toString()
                            if (note in VALID_NOTES) {
                                Log.d("BluetoothViewModel", "Emitting valid note: $note")
                                _incomingNote.emit(note)
                            } else {
                                Log.d("BluetoothViewModel", "Ignoring invalid char: $note")
                            }
                        }
                    }
                } catch (e: IOException) {
                    Log.e("BluetoothViewModel", "Disconnected or error reading stream", e)
                    break
                }
            }

            _isConnected.value = false
            Log.d("BluetoothViewModel", "Listening job finished. Connection state set to false.")
        }
    }

    fun disconnect() {
        listeningJob?.cancel()
        listeningJob = null
        try {
            bluetoothSocket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            bluetoothSocket = null
            _isConnected.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}