package com.example.harpapp.device.bluetooth

import android.Manifest
import org.junit.Assert.assertEquals
import org.junit.Test

class BluetoothPermissionsTest {

    @Test
    fun usesLocationPermissionBeforeAndroid12() {
        assertEquals(
            listOf(Manifest.permission.ACCESS_FINE_LOCATION),
            BluetoothPermissions.requiredRuntimePermissions(sdkInt = 29)
        )
        assertEquals(
            listOf(Manifest.permission.ACCESS_FINE_LOCATION),
            BluetoothPermissions.requiredRuntimePermissions(sdkInt = 30)
        )
    }

    @Test
    fun usesBluetoothPermissionsOnAndroid12AndNewer() {
        assertEquals(
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            ),
            BluetoothPermissions.requiredRuntimePermissions(sdkInt = 31)
        )
        assertEquals(
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            ),
            BluetoothPermissions.requiredRuntimePermissions(sdkInt = 33)
        )
    }
}
