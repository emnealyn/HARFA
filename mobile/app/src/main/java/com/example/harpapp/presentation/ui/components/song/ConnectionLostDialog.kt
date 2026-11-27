package com.example.harpapp.presentation.ui.components.song

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.harpapp.R

@Composable
fun ConnectionLostDialog(
    onGoToBluetooth: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.BluetoothDisabled,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text(stringResource(R.string.connection_lost_dialog_title)) },
        text = { Text(stringResource(R.string.connection_lost_dialog_message)) },
        confirmButton = {
            Button(onClick = onGoToBluetooth) {
                Text(stringResource(R.string.bluetooth_settings))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        }
    )
}

@Preview
@Composable
fun ConnectionLostDialogPreview() {
    ConnectionLostDialog(onGoToBluetooth = {}, onDismiss = {})
}
