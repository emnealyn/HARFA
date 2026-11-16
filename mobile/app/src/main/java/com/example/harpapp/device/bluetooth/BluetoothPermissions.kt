package com.example.harpapp.device.bluetooth

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

internal object BluetoothPermissions {

    fun requiredRuntimePermissions(sdkInt: Int = Build.VERSION.SDK_INT): List<String> =
        if (sdkInt >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    fun requiredConnectPermissions(sdkInt: Int = Build.VERSION.SDK_INT): List<String> =
        if (sdkInt >= Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            emptyList()
        }

    fun missingPermissions(
        context: Context,
        sdkInt: Int = Build.VERSION.SDK_INT
    ): List<String> = missingPermissions(context, requiredRuntimePermissions(sdkInt))

    fun missingPermissions(
        context: Context,
        permissions: Collection<String>
    ): List<String> {
        return permissions.filter { permission ->
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        }
    }

    fun requireRuntimePermissions(context: Context) {
        val missing = missingPermissions(context)
        if (missing.isNotEmpty()) {
            throw BluetoothPermissionRequiredException(missing)
        }
    }

    fun requireConnectPermission(context: Context) {
        val missing = missingPermissions(context, requiredConnectPermissions())
        if (missing.isNotEmpty()) {
            throw BluetoothPermissionRequiredException(missing)
        }
    }
}

internal class BluetoothPermissionRequiredException(
    val missingPermissions: List<String>
) : SecurityException(
    "Missing Bluetooth permissions: ${missingPermissions.joinToString()}"
)
