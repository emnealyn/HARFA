package com.example.harpapp.viewmodel

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID

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

    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val TARGET_MAC_ADDRESSES = listOf(
        "20:E7:C8:5A:67:4E"
    )

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

                val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                bluetoothSocket = socket
                socket.connect()

                val nameToSave = device.name ?: device.address
                _isConnected.value = true
                _connectedDeviceName.value = nameToSave
                prefs.edit().putString("last_device_name", nameToSave).apply()
            } catch (e: IOException) {
                e.printStackTrace()
                _isConnected.value = false
                val lastDevice = prefs.getString("last_device_name", null)
                _connectedDeviceName.value = lastDevice
            }
        }
    }

    fun disconnect() {
        try {
            bluetoothSocket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            bluetoothSocket = null
            _isConnected.value = false
            // We do not clear _connectedDeviceName here because we want to remember it
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}
