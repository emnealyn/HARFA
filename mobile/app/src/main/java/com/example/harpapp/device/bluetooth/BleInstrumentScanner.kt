package com.example.harpapp.device.bluetooth

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import com.example.harpapp.domain.model.InstrumentDevice
import com.example.harpapp.protocol.HarpProtocol
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class BleInstrumentScanner(
    context: Context
) {

    private val appContext = context.applicationContext
    private val bluetoothManager = appContext
        .getSystemService(BluetoothManager::class.java)

    suspend fun scan(timeoutMs: Long = DEFAULT_SCAN_TIMEOUT_MS): List<InstrumentDevice> {
        require(timeoutMs > 0) { "Scan timeout must be greater than zero" }
        BluetoothPermissions.requireRuntimePermissions(appContext)

        val devices = LinkedHashMap<String, InstrumentDevice>()
        withTimeoutOrNull(timeoutMs) {
            scanUntilCancelled(devices)
        }

        return devices.values.toList()
    }

    private suspend fun scanUntilCancelled(
        devices: MutableMap<String, InstrumentDevice>
    ) {
        suspendCancellableCoroutine<Unit> { continuation ->
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

            val bluetoothAdapter = bluetoothManager?.adapter
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                continuation.resumeWithException(
                    BluetoothUnavailableException(
                        "Bluetooth adapter is unavailable or disabled"
                    )
                )
                return@suspendCancellableCoroutine
            }

            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                continuation.resumeWithException(
                    BluetoothPermissionRequiredException(
                        listOf(Manifest.permission.BLUETOOTH_SCAN)
                    )
                )
                return@suspendCancellableCoroutine
            }

            val scanner = bluetoothAdapter.bluetoothLeScanner
            if (scanner == null) {
                continuation.resumeWithException(
                    IllegalStateException("BLE scanner is unavailable")
                )
                return@suspendCancellableCoroutine
            }

            val callback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    if (!continuation.isActive) return
                    addResult(devices, result)
                }

                override fun onBatchScanResults(results: MutableList<ScanResult>) {
                    if (!continuation.isActive) return
                    results.forEach { result -> addResult(devices, result) }
                }

                override fun onScanFailed(errorCode: Int) {
                    if (
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                        ContextCompat.checkSelfPermission(
                            appContext,
                            Manifest.permission.BLUETOOTH_SCAN
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        scanner.stopScan(this)
                    }
                    if (continuation.isActive) {
                        continuation.resumeWithException(
                            IllegalStateException("BLE scan failed: errorCode=$errorCode")
                        )
                    }
                }
            }

            continuation.invokeOnCancellation {
                if (
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                    ContextCompat.checkSelfPermission(
                        appContext,
                        Manifest.permission.BLUETOOTH_SCAN
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    scanner.stopScan(callback)
                }
            }

            try {
                scanner.startScan(
                    listOf(
                        ScanFilter.Builder()
                            .setServiceUuid(ParcelUuid(HarpProtocol.SERVICE_UUID))
                            .build()
                    ),
                    ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build(),
                    callback
                )
            } catch (exception: SecurityException) {
                continuation.resumeWithException(exception)
            }
        }
    }

    private fun addResult(
        devices: MutableMap<String, InstrumentDevice>,
        result: ScanResult
    ) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val device = result.device ?: return
        val address = try {
            device.address
        } catch (_: SecurityException) {
            return
        }

        val name = try {
            result.scanRecord?.deviceName ?: device.name
        } catch (_: SecurityException) {
            result.scanRecord?.deviceName
        }

        devices[address] = InstrumentDevice(
            name = name,
            address = address
        )
    }

    private companion object {
        const val DEFAULT_SCAN_TIMEOUT_MS = 10_000L
    }
}
