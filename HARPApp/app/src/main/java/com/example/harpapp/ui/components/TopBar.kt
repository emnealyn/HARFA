package com.example.harpapp.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.harpapp.R
import com.example.harpapp.ui.theme.HARPAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    title: String,
    onBackClick: () -> Unit,
    isBluetoothConnected: Boolean,
    onBluetoothClick: () -> Unit,
    modifier: Modifier = Modifier,
    connectedDeviceName: String? = null
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        actions = {
            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                if (connectedDeviceName != null) {
                    Text(
                        text = connectedDeviceName,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isBluetoothConnected) {
                            Color.Green.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                        },
                        modifier = Modifier
                    )
                }

                IconButton(
                    onClick = onBluetoothClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isBluetoothConnected) {
                            Icons.Default.BluetoothConnected
                        } else {
                            Icons.Default.BluetoothDisabled
                        },
                        contentDescription = stringResource(R.string.bluetooth_status),
                        tint = if (isBluetoothConnected) {
                            Color.Green
                        } else {
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                        }
                    )
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Preview
@Composable
fun HarpTopAppBarConnectedPreview() {
    HARPAppTheme {
        TopBar(
            title = "Wildest Dreams",
            onBackClick = {},
            isBluetoothConnected = true,
            onBluetoothClick = {}
        )
    }
}
