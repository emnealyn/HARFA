package com.example.harpapp.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.example.harpapp.ui.theme.DarkRed
import com.example.harpapp.ui.theme.HARPAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    title: String,
    onBackClick: () -> Unit,
    isBluetoothConnected: Boolean,
    onBluetoothClick: () -> Unit,
    modifier: Modifier = Modifier
    ){
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        },
        actions = {
            IconButton(onClick = onBluetoothClick) {
                Icon(
                    imageVector = if (isBluetoothConnected) {
                        Icons.Default.BluetoothConnected
                    } else {
                        Icons.Default.BluetoothDisabled
                    },
                    contentDescription = "Status Bluetooth",
                    tint = if (isBluetoothConnected) {
                        Color.Green
                    } else {
                        Color.White.copy(alpha = 0.5f)
                    }
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = DarkRed
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

