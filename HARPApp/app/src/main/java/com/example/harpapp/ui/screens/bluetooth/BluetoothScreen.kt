package com.example.harpapp.ui.screens.bluetooth

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.harpapp.viewmodel.BluetoothViewModel
import android.annotation.SuppressLint

@SuppressLint("MissingPermission")
@Composable
fun BluetoothScreen(
    viewModel: BluetoothViewModel = viewModel()
) {
    val context = LocalContext.current
    val isConnected by viewModel.isConnected.collectAsState()
    val connectedDeviceName by viewModel.connectedDeviceName.collectAsState()
    val pairedDevices by viewModel.pairedDevices.collectAsState()

    var hasPermissions by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermissions = permissions.values.all { it }
        if (hasPermissions) {
            viewModel.fetchPairedDevices(context)
        }
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
        }
        permissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Status: ${if (isConnected) "Połączono" else "Rozłączono"}",
            style = MaterialTheme.typography.titleLarge
        )

        if (isConnected) {
            Text(
                text = "Urządzenie: ${connectedDeviceName ?: "Nieznane"}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
            Button(
                onClick = { viewModel.disconnect() },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Rozłącz")
            }
        } else {
            Text(
                text = "Sparowane urządzenia:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )

            if (!hasPermissions) {
                Text("Brak uprawnień do Bluetooth")
            } else {
                Button(
                    onClick = { viewModel.fetchPairedDevices(context) },
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text("Odśwież listę")
                }

                LazyColumn {
                    items(pairedDevices) { device ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { viewModel.connectToDevice(device) }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = device.name ?: "Nieznane urządzenie", style = MaterialTheme.typography.bodyLarge)
                                Text(text = device.address, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}