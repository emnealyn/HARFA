package com.example.harpapp.device.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import com.example.harpapp.domain.model.PairedDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

@SuppressLint("MissingPermission")
internal class RfcommInstrumentTransport(
    context: Context
) : InstrumentTransport {

    private val appContext = context.applicationContext
    private val bluetoothManager = appContext.getSystemService(BluetoothManager::class.java)
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _events = MutableSharedFlow<TransportEvent>(extraBufferCapacity = 16)
    override val events: Flow<TransportEvent> = _events

    private var bluetoothSocket: BluetoothSocket? = null
    private var listeningJob: Job? = null

    override suspend fun connect(device: PairedDevice) {
        withContext(Dispatchers.IO) {
            disconnect()

            val bluetoothAdapter = bluetoothManager?.adapter
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                throw IllegalStateException("Bluetooth adapter is unavailable or disabled")
            }

            val bluetoothDevice = bluetoothAdapter.getRemoteDevice(device.address)
            val socket = try {
                bluetoothAdapter.cancelDiscovery()
                bluetoothDevice.createRfcommSocketToServiceRecord(SPP_UUID)
            } catch (exception: SecurityException) {
                throw exception
            }

            try {
                socket.connect()
                bluetoothSocket = socket
                startListening(socket)
            } catch (exception: IOException) {
                closeSocket(socket)
                throw exception
            } catch (exception: SecurityException) {
                closeSocket(socket)
                throw exception
            }
        }
    }

    override fun disconnect() {
        listeningJob?.cancel()
        listeningJob = null

        val socket = bluetoothSocket
        bluetoothSocket = null
        closeSocket(socket)
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

                    _events.emit(
                        TransportEvent.Data(buffer.copyOf(bytesRead))
                    )
                }
            } catch (exception: IOException) {
                Log.e(TAG, "Disconnected or error reading Bluetooth stream", exception)
            } catch (exception: SecurityException) {
                Log.e(TAG, "Bluetooth permissions are missing", exception)
            } finally {
                if (socket == bluetoothSocket) {
                    bluetoothSocket = null
                    _events.emit(TransportEvent.Disconnected)
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

    private companion object {
        const val TAG = "RfcommInstrumentTransport"
        const val BUFFER_SIZE = 1024
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
}
