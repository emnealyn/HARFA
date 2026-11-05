package com.example.harpapp.device.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import android.util.Log
import com.example.harpapp.domain.model.PairedDevice
import com.example.harpapp.protocol.HarpProtocol
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@SuppressLint("MissingPermission")
internal class BleGattInstrumentTransport(
    context: Context
) : InstrumentTransport {

    private val appContext = context.applicationContext
    private val bluetoothManager = appContext.getSystemService(BluetoothManager::class.java)

    private val _events = MutableSharedFlow<TransportEvent>(extraBufferCapacity = 16)
    override val events: Flow<TransportEvent> = _events

    private var bluetoothGatt: BluetoothGatt? = null
    private var connectContinuation: CancellableContinuation<Unit>? = null

    override suspend fun connect(device: PairedDevice) {
        suspendCancellableCoroutine { continuation ->
            disconnect()
            connectContinuation = continuation

            val bluetoothAdapter = bluetoothManager?.adapter
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                failConnection(IllegalStateException("Bluetooth adapter is unavailable or disabled"))
                return@suspendCancellableCoroutine
            }

            val bluetoothDevice = try {
                bluetoothAdapter.getRemoteDevice(device.address)
            } catch (exception: IllegalArgumentException) {
                failConnection(exception)
                return@suspendCancellableCoroutine
            } catch (exception: SecurityException) {
                failConnection(exception)
                return@suspendCancellableCoroutine
            }

            continuation.invokeOnCancellation {
                disconnect()
            }

            try {
                val gatt = bluetoothDevice.connectGatt(
                    appContext,
                    false,
                    gattCallback,
                    BluetoothDevice.TRANSPORT_LE
                )
                if (gatt == null) {
                    failConnection(IOException("Could not create Bluetooth GATT connection"))
                } else {
                    bluetoothGatt = gatt
                }
            } catch (exception: SecurityException) {
                failConnection(exception)
            }
        }
    }

    override fun disconnect() {
        val hadConnection = bluetoothGatt != null || connectContinuation != null
        val gatt = bluetoothGatt
        bluetoothGatt = null

        val continuation = connectContinuation
        connectContinuation = null
        if (continuation?.isActive == true) {
            continuation.resumeWithException(
                CancellationException("BLE GATT connection was cancelled")
            )
        }

        gatt?.disconnect()
        gatt?.close()

        if (hadConnection) {
            _events.tryEmit(TransportEvent.Disconnected)
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(
            gatt: BluetoothGatt,
            status: Int,
            newState: Int
        ) {
            if (gatt !== bluetoothGatt) {
                gatt.close()
                return
            }

            if (status != BluetoothGatt.GATT_SUCCESS || newState == BluetoothProfile.STATE_DISCONNECTED) {
                val error = IOException(
                    "BLE GATT connection failed: status=$status, state=$newState"
                )
                val wasConnecting = connectContinuation != null
                closeGatt(gatt)
                if (wasConnecting) {
                    resumeConnectionWithException(error)
                } else {
                    _events.tryEmit(TransportEvent.Disconnected)
                }
                return
            }

            if (newState == BluetoothProfile.STATE_CONNECTED && !gatt.discoverServices()) {
                failConnection(IOException("Could not start GATT service discovery"))
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (gatt !== bluetoothGatt) return

            if (status != BluetoothGatt.GATT_SUCCESS) {
                failConnection(IOException("GATT service discovery failed: status=$status"))
                return
            }

            val service = gatt.getService(HarpProtocol.SERVICE_UUID)
            val rx = service?.getCharacteristic(HarpProtocol.RX_CHARACTERISTIC_UUID)
            val tx = service?.getCharacteristic(HarpProtocol.TX_CHARACTERISTIC_UUID)

            if (service == null || rx == null || tx == null) {
                failConnection(IllegalStateException("HARFA GATT service or characteristics are missing"))
                return
            }

            val canWrite = rx.properties and (
                BluetoothGattCharacteristic.PROPERTY_WRITE or
                    BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE
                ) != 0
            val canNotify = tx.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0
            val cccd = tx.getDescriptor(CCCD_UUID)

            if (!canWrite || !canNotify || cccd == null) {
                failConnection(IllegalStateException("HARFA GATT characteristics have invalid properties"))
                return
            }

            if (!gatt.setCharacteristicNotification(tx, true)) {
                failConnection(IOException("Could not enable TX notifications"))
                return
            }

            writeNotificationDescriptor(gatt, cccd)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (gatt !== bluetoothGatt || descriptor.uuid != CCCD_UUID) return

            if (status != BluetoothGatt.GATT_SUCCESS) {
                failConnection(IOException("Could not write TX CCCD: status=$status"))
                return
            }

            val continuation = connectContinuation ?: return
            connectContinuation = null
            continuation.resume(Unit)
        }

        @Suppress("DEPRECATION")
        @Deprecated("Use the overload that provides the notification value")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            publishNotification(gatt, characteristic, characteristic.value)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            publishNotification(gatt, characteristic, value)
        }
    }

    private fun writeNotificationDescriptor(
        gatt: BluetoothGatt,
        descriptor: BluetoothGattDescriptor
    ) {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        } else {
            @Suppress("DEPRECATION")
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            @Suppress("DEPRECATION")
            if (gatt.writeDescriptor(descriptor)) {
                BluetoothGatt.GATT_SUCCESS
            } else {
                BluetoothGatt.GATT_FAILURE
            }
        }

        if (result != BluetoothGatt.GATT_SUCCESS) {
            failConnection(IOException("Could not start TX CCCD write: status=$result"))
        }
    }

    private fun publishNotification(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray?
    ) {
        if (gatt !== bluetoothGatt || characteristic.uuid != HarpProtocol.TX_CHARACTERISTIC_UUID) {
            return
        }

        val bytes = value ?: return
        if (!_events.tryEmit(TransportEvent.Data(bytes.copyOf()))) {
            Log.e(TAG, "Dropping BLE notification because the event buffer is full")
        }
    }

    private fun failConnection(error: Throwable) {
        closeGatt(bluetoothGatt)
        resumeConnectionWithException(error)
    }

    private fun resumeConnectionWithException(error: Throwable) {
        val continuation = connectContinuation ?: return
        connectContinuation = null
        if (continuation.isActive) {
            continuation.resumeWithException(error)
        }
    }

    private fun closeGatt(gatt: BluetoothGatt?) {
        if (gatt === bluetoothGatt) {
            bluetoothGatt = null
        }
        gatt?.disconnect()
        gatt?.close()
    }

    private companion object {
        const val TAG = "BleGattInstrumentTransport"
        val CCCD_UUID = java.util.UUID.fromString(
            "00002902-0000-1000-8000-00805F9B34FB"
        )
    }
}
