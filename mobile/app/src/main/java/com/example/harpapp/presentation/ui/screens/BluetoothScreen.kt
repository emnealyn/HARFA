package com.example.harpapp.presentation.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.harpapp.R
import com.example.harpapp.device.bluetooth.BluetoothPermissions
import com.example.harpapp.domain.model.ConnectionErrorCode
import com.example.harpapp.domain.model.ConnectionState
import com.example.harpapp.domain.model.InstrumentDevice
import com.example.harpapp.domain.model.isOperationInProgress
import com.example.harpapp.presentation.viewmodel.BluetoothViewModel

private sealed interface BluetoothPermissionAction {
    data object Scan : BluetoothPermissionAction
    data class Connect(val device: InstrumentDevice) : BluetoothPermissionAction
}

@Composable
fun BluetoothScreen(
    viewModel: BluetoothViewModel
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val connectionState by viewModel.connectionState.collectAsState(
        initial = ConnectionState.DISCONNECTED
    )
    val currentConnectionState = connectionState
    val connectedDeviceName by viewModel.connectedDeviceName.collectAsState(initial = null)
    val discoveredDevices by viewModel.discoveredDevices.collectAsState(initial = emptyList())

    var missingPermissions by remember {
        mutableStateOf(BluetoothPermissions.missingPermissions(context))
    }
    var permissionRequestAttempted by rememberSaveable { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<BluetoothPermissionAction?>(null) }
    var rationaleAction by remember { mutableStateOf<BluetoothPermissionAction?>(null) }

    fun refreshPermissions() {
        missingPermissions = BluetoothPermissions.missingPermissions(context)
    }

    fun execute(action: BluetoothPermissionAction) {
        pendingAction = null
        when (action) {
            BluetoothPermissionAction.Scan -> viewModel.scanForDevices()
            is BluetoothPermissionAction.Connect -> viewModel.connectToDevice(action.device)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permissionRequestAttempted = true
        val updatedMissingPermissions = BluetoothPermissions.missingPermissions(context)
        missingPermissions = updatedMissingPermissions
        if (updatedMissingPermissions.isEmpty()) {
            pendingAction?.let(::execute)
        }
    }

    fun requestPermissions(action: BluetoothPermissionAction) {
        val currentMissingPermissions = BluetoothPermissions.missingPermissions(context)
        missingPermissions = currentMissingPermissions
        pendingAction = action

        if (currentMissingPermissions.isEmpty()) {
            execute(action)
        } else if (
            activity != null &&
            currentMissingPermissions.any {
                ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
            }
        ) {
            rationaleAction = action
        } else {
            permissionLauncher.launch(currentMissingPermissions.toTypedArray())
        }
    }

    fun openApplicationSettings() {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        )
    }

    fun openBluetoothSettings() {
        context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val hasPermissions = missingPermissions.isEmpty()
    val showsPermissionRationale = activity != null &&
        missingPermissions.any {
            ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
        }
    val permanentlyDenied = permissionRequestAttempted &&
        missingPermissions.isNotEmpty() &&
        !showsPermissionRationale
    val operationInProgress = currentConnectionState.isOperationInProgress
    val canStartOperation =
        !operationInProgress && currentConnectionState != ConnectionState.CONNECTED

    val statusColor = when {
        currentConnectionState == ConnectionState.CONNECTED ->
            Color(0xFF3B8A40)
        operationInProgress ->
            MaterialTheme.colorScheme.primary
        currentConnectionState == ConnectionState.DISCONNECTED ->
            MaterialTheme.colorScheme.onSurfaceVariant
        else ->
            MaterialTheme.colorScheme.error
    }
    val statusIcon = when {
        currentConnectionState == ConnectionState.CONNECTED -> Icons.Default.BluetoothConnected
        currentConnectionState == ConnectionState.DISCONNECTED -> Icons.Default.Bluetooth
        operationInProgress -> Icons.Default.Bluetooth
        else -> Icons.Default.BluetoothDisabled
    }
    val statusTitle = when (currentConnectionState) {
        ConnectionState.BLUETOOTH_DISABLED ->
            stringResource(R.string.connection_bluetooth_disabled)
        ConnectionState.PERMISSION_REQUIRED ->
            stringResource(R.string.connection_permission_required)
        ConnectionState.SCANNING ->
            stringResource(R.string.connection_scanning)
        ConnectionState.CONNECTING ->
            stringResource(R.string.connection_connecting)
        ConnectionState.DISCOVERING_SERVICES ->
            stringResource(R.string.connection_discovering_services)
        ConnectionState.HANDSHAKING ->
            stringResource(R.string.connection_handshaking)
        ConnectionState.CONNECTED ->
            stringResource(R.string.connected)
        ConnectionState.DISCONNECTING ->
            stringResource(R.string.connection_disconnecting)
        ConnectionState.DISCONNECTED ->
            stringResource(R.string.disconnected)
        is ConnectionState.ERROR ->
            stringResource(connectionErrorMessage(currentConnectionState.error.code))
    }
    val statusMessage = when (currentConnectionState) {
        ConnectionState.CONNECTED ->
            connectedDeviceName ?: stringResource(R.string.no_device)
        ConnectionState.BLUETOOTH_DISABLED ->
            stringResource(R.string.connection_bluetooth_disabled_message)
        ConnectionState.PERMISSION_REQUIRED ->
            stringResource(R.string.bluetooth_permissions_are_required)
        ConnectionState.SCANNING ->
            stringResource(R.string.connection_scanning_message)
        ConnectionState.CONNECTING ->
            stringResource(R.string.connection_connecting_message)
        ConnectionState.DISCOVERING_SERVICES ->
            stringResource(R.string.connection_discovering_services_message)
        ConnectionState.HANDSHAKING ->
            stringResource(R.string.connection_handshaking_message)
        ConnectionState.DISCONNECTING ->
            stringResource(R.string.connection_disconnecting_message)
        ConnectionState.DISCONNECTED ->
            stringResource(R.string.no_device)
        is ConnectionState.ERROR ->
            stringResource(R.string.connection_error_message)
    }

    rationaleAction?.let { action ->
        AlertDialog(
            onDismissRequest = {
                rationaleAction = null
                pendingAction = null
            },
            title = { Text(stringResource(R.string.bluetooth_permissions_rationale_title)) },
            text = { Text(stringResource(R.string.bluetooth_permissions_rationale_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        rationaleAction = null
                        val currentMissingPermissions =
                            BluetoothPermissions.missingPermissions(context)
                        if (currentMissingPermissions.isEmpty()) {
                            execute(action)
                        } else {
                            permissionLauncher.launch(currentMissingPermissions.toTypedArray())
                        }
                    }
                ) {
                    Text(stringResource(R.string.grant_permissions))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        rationaleAction = null
                        pendingAction = null
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .widthIn(max = 600.dp)
                .fillMaxHeight(),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = statusColor.copy(alpha = 0.1f)
                    ),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(32.dp)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = statusTitle,
                                style = MaterialTheme.typography.titleLarge,
                                color = statusColor
                            )
                            Text(
                                text = statusMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        when {
                            currentConnectionState == ConnectionState.CONNECTED -> {
                                IconButton(onClick = viewModel::disconnect) {
                                    Icon(
                                        imageVector = Icons.Default.LinkOff,
                                        contentDescription = stringResource(R.string.disconnect),
                                        tint = statusColor
                                    )
                                }
                            }
                            operationInProgress &&
                                currentConnectionState != ConnectionState.DISCONNECTING -> {
                                IconButton(onClick = viewModel::disconnect) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.cancel),
                                        tint = statusColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = stringResource(R.string.discovered_devices),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = { requestPermissions(BluetoothPermissionAction.Scan) },
                        enabled = canStartOperation
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.refresh),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (!hasPermissions) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = if (permanentlyDenied) {
                                    stringResource(R.string.bluetooth_permissions_permanently_denied)
                                } else {
                                    stringResource(R.string.bluetooth_permissions_are_required)
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        if (permanentlyDenied) {
                                            openApplicationSettings()
                                        } else {
                                            requestPermissions(BluetoothPermissionAction.Scan)
                                        }
                                    }
                                ) {
                                    Text(
                                        stringResource(
                                            if (permanentlyDenied) {
                                                R.string.open_app_settings
                                            } else {
                                                R.string.grant_permissions
                                            }
                                        )
                                    )
                                }
                                if (permanentlyDenied) {
                                    OutlinedButton(onClick = ::refreshPermissions) {
                                        Text(stringResource(R.string.check_again))
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (currentConnectionState == ConnectionState.BLUETOOTH_DISABLED) {
                item {
                    StatusCard(
                        text = stringResource(R.string.connection_bluetooth_disabled_message),
                        actionLabel = stringResource(R.string.open_bluetooth_settings),
                        onAction = ::openBluetoothSettings
                    )
                }
            } else if (currentConnectionState is ConnectionState.ERROR) {
                item {
                    StatusCard(
                        text = statusMessage,
                        actionLabel = stringResource(R.string.retry),
                        onAction = { requestPermissions(BluetoothPermissionAction.Scan) },
                        isError = true
                    )
                }
            } else if (!operationInProgress && discoveredDevices.isEmpty()) {
                item {
                    StatusCard(text = stringResource(R.string.no_discovered_devices_found))
                }
            }

            items(discoveredDevices) { device ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            enabled = canStartOperation,
                            onClick = {
                                requestPermissions(BluetoothPermissionAction.Connect(device))
                            }
                        ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Piano,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = device.name ?: stringResource(R.string.unknown_device),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = stringResource(R.string.connect),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusCard(
    text: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    isError: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(
            1.dp,
            if (isError) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = text,
                color = if (isError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}

private fun connectionErrorMessage(code: ConnectionErrorCode): Int = when (code) {
    ConnectionErrorCode.SCAN_FAILED -> R.string.bluetooth_scan_failed
    ConnectionErrorCode.CONNECTION_FAILED -> R.string.bluetooth_connection_failed
    ConnectionErrorCode.SERVICE_DISCOVERY_FAILED -> R.string.bluetooth_service_discovery_failed
    ConnectionErrorCode.HANDSHAKE_FAILED -> R.string.bluetooth_handshake_failed
    ConnectionErrorCode.TIMEOUT -> R.string.bluetooth_connection_timeout
    ConnectionErrorCode.UNKNOWN -> R.string.connection_error_message
}
