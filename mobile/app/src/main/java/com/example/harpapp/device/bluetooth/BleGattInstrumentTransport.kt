package com.example.harpapp.device.bluetooth

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.harpapp.domain.model.InstrumentDevice
import com.example.harpapp.protocol.HarpProtocol
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class BleGattInstrumentTransport(
    context: Context
) : InstrumentTransport {

    private val appContext = context.applicationContext
    private val bluetoothManager = appContext.getSystemService(BluetoothManager::class.java)

    private val _events = MutableSharedFlow<TransportEvent>(extraBufferCapacity = 16)
    override val events: Flow<TransportEvent> = _events

    private var bluetoothGatt: BluetoothGatt? = null
    private var connectContinuation: CancellableContinuation<Unit>? = null
    private var writeContinuation: CancellableContinuation<Unit>? = null
    private var rxCharacteristic: BluetoothGattCharacteristic? = null
    @Volatile
    private var connectionStageCallback: ((TransportConnectionStage) -> Unit)? = null

    override suspend fun connect(
        device: InstrumentDevice,
        onStage: (TransportConnectionStage) -> Unit
    ) {
        BluetoothPermissions.requireConnectPermission(appContext)
        onStage(TransportConnectionStage.CONNECTING)
        try {
            withTimeout(CONNECTION_TIMEOUT_MS) {
                suspendCancellableCoroutine<Unit> { continuation ->
                    disconnectGatt(notify = false)
                    connectionStageCallback = onStage
                    connectContinuation = continuation

                    if (
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        ContextCompat.checkSelfPermission(
                            appContext,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        failConnection(
                            BluetoothPermissionRequiredException(
                                listOf(Manifest.permission.BLUETOOTH_CONNECT)
                            )
                        )
                        return@suspendCancellableCoroutine
                    }

                    val bluetoothAdapter = bluetoothManager?.adapter
                    if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                        failConnection(
                            BluetoothUnavailableException(
                                "Bluetooth adapter is unavailable or disabled"
                            )
                        )
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
        } catch (exception: TimeoutCancellationException) {
            disconnect()
            throw BleConnectionTimeoutException("BLE GATT connection timed out")
        }
    }

    override suspend fun send(bytes: ByteArray) {
        require(bytes.isNotEmpty()) { "Cannot send an empty BLE frame" }
        require(bytes.size <= HarpProtocol.MAX_FRAME_SIZE) {
            "BLE frame cannot exceed ${HarpProtocol.MAX_FRAME_SIZE} bytes"
        }
        BluetoothPermissions.requireConnectPermission(appContext)

        suspendCancellableCoroutine { continuation ->
            val gatt = bluetoothGatt
            val rx = rxCharacteristic
            if (gatt == null || rx == null) {
                continuation.resumeWithException(
                    IllegalStateException("BLE GATT RX characteristic is unavailable")
                )
                return@suspendCancellableCoroutine
            }

            if (writeContinuation != null) {
                continuation.resumeWithException(
                    IllegalStateException("Another BLE write is already pending")
                )
                return@suspendCancellableCoroutine
            }

            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                continuation.resumeWithException(
                    BluetoothPermissionRequiredException(
                        listOf(Manifest.permission.BLUETOOTH_CONNECT)
                    )
                )
                return@suspendCancellableCoroutine
            }

            writeContinuation = continuation
            continuation.invokeOnCancellation {
                if (writeContinuation === continuation) {
                    writeContinuation = null
                }
            }

            val writeType = if (
                rx.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0
            ) {
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            } else {
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            }

            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeCharacteristic(rx, bytes, writeType)
            } else {
                @Suppress("DEPRECATION")
                rx.writeType = writeType
                @Suppress("DEPRECATION")
                rx.value = bytes
                @Suppress("DEPRECATION")
                if (gatt.writeCharacteristic(rx)) {
                    BluetoothGatt.GATT_SUCCESS
                } else {
                    BluetoothGatt.GATT_FAILURE
                }
            }

            if (result != BluetoothGatt.GATT_SUCCESS) {
                resumeWriteWithException(
                    IOException("Could not start RX write: status=$result")
                )
            } else if (writeType == BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) {
                val completedContinuation = writeContinuation
                writeContinuation = null
                completedContinuation?.resume(Unit)
            }
        }
    }

    override fun disconnect() {
        disconnectGatt(notify = true)
    }

    private fun disconnectGatt(notify: Boolean) {
        val hadConnection = bluetoothGatt != null || connectContinuation != null
        val gatt = bluetoothGatt
        bluetoothGatt = null
        rxCharacteristic = null
        connectionStageCallback = null

        val continuation = connectContinuation
        connectContinuation = null
        if (continuation?.isActive == true) {
            continuation.resumeWithException(
                CancellationException("BLE GATT connection was cancelled")
            )
        }
        resumeWriteWithException(CancellationException("BLE GATT connection was cancelled"))

        if (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            gatt?.disconnect()
        }
        closeGattHandle(gatt)

        if (notify && hadConnection) {
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
                closeGattHandle(gatt)
                return
            }

            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                failConnection(
                    BluetoothPermissionRequiredException(
                        listOf(Manifest.permission.BLUETOOTH_CONNECT)
                    )
                )
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

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (!gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)) {
                    Log.w(TAG, "Could not request high BLE connection priority")
                }
                connectionStageCallback?.invoke(TransportConnectionStage.DISCOVERING_SERVICES)
                if (!gatt.discoverServices()) {
                    failConnection(
                        BleServiceDiscoveryException("Could not start GATT service discovery")
                    )
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (gatt !== bluetoothGatt) return

            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                failConnection(
                    BluetoothPermissionRequiredException(
                        listOf(Manifest.permission.BLUETOOTH_CONNECT)
                    )
                )
                return
            }

            if (status != BluetoothGatt.GATT_SUCCESS) {
                failConnection(
                    BleServiceDiscoveryException(
                        "GATT service discovery failed: status=$status"
                    )
                )
                return
            }

            val service = gatt.getService(HarpProtocol.SERVICE_UUID)
            val rx = service?.getCharacteristic(HarpProtocol.RX_CHARACTERISTIC_UUID)
            val tx = service?.getCharacteristic(HarpProtocol.TX_CHARACTERISTIC_UUID)

            if (service == null || rx == null || tx == null) {
                failConnection(
                    BleServiceDiscoveryException(
                        "HARFA GATT service or characteristics are missing"
                    )
                )
                return
            }

            val canWrite = rx.properties and (
                BluetoothGattCharacteristic.PROPERTY_WRITE or
                    BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE
                ) != 0
            val canNotify = tx.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0
            val cccd = tx.getDescriptor(CCCD_UUID)

            if (!canWrite || !canNotify || cccd == null) {
                failConnection(
                    BleServiceDiscoveryException(
                        "HARFA GATT characteristics have invalid properties"
                    )
                )
                return
            }

            rxCharacteristic = rx

            if (!gatt.setCharacteristicNotification(tx, true)) {
                failConnection(BleServiceDiscoveryException("Could not enable TX notifications"))
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

            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                failConnection(
                    BluetoothPermissionRequiredException(
                        listOf(Manifest.permission.BLUETOOTH_CONNECT)
                    )
                )
                return
            }

            if (status != BluetoothGatt.GATT_SUCCESS) {
                failConnection(
                    BleServiceDiscoveryException(
                        "Could not write TX CCCD: status=$status"
                    )
                )
                return
            }

            connectionStageCallback?.invoke(TransportConnectionStage.READY_FOR_HANDSHAKE)
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

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (gatt !== bluetoothGatt || characteristic !== rxCharacteristic) return

            if (status == BluetoothGatt.GATT_SUCCESS) {
                val continuation = writeContinuation ?: return
                writeContinuation = null
                continuation.resume(Unit)
            } else {
                resumeWriteWithException(
                    IOException("RX write failed: status=$status")
                )
            }
        }
    }

    private fun writeNotificationDescriptor(
        gatt: BluetoothGatt,
        descriptor: BluetoothGattDescriptor
    ) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            failConnection(
                BluetoothPermissionRequiredException(
                    listOf(Manifest.permission.BLUETOOTH_CONNECT)
                )
            )
            return
        }

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
            failConnection(
                BleServiceDiscoveryException("Could not start TX CCCD write: status=$result")
            )
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
        resumeWriteWithException(error)
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
            rxCharacteristic = null
            connectionStageCallback = null
        }
        if (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            gatt?.disconnect()
        }
        closeGattHandle(gatt)
    }

    private fun closeGattHandle(gatt: BluetoothGatt?) {
        try {
            gatt?.close()
        } catch (exception: SecurityException) {
            Log.w(TAG, "Could not close BLE GATT after permission loss", exception)
        }
    }

    private fun resumeWriteWithException(error: Throwable) {
        val continuation = writeContinuation ?: return
        writeContinuation = null
        if (continuation.isActive) {
            continuation.resumeWithException(error)
        }
    }

    private companion object {
        const val TAG = "BleGattInstrumentTransport"
        const val CONNECTION_TIMEOUT_MS = 15_000L
        val CCCD_UUID = java.util.UUID.fromString(
            "00002902-0000-1000-8000-00805F9B34FB"
        )
    }
}
