package com.example.harpapp.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.harpapp.R
import com.example.harpapp.domain.model.BatteryStatus
import com.example.harpapp.presentation.ui.theme.HARPAppTheme
import com.example.harpapp.presentation.ui.theme.TopBarTitleTextStyle
import com.example.harpapp.presentation.ui.theme.extendedColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    title: String,
    onBackClick: () -> Unit,
    isBluetoothConnected: Boolean,
    onBluetoothClick: () -> Unit,
    modifier: Modifier = Modifier,
    connectedDeviceName: String? = null,
    batteryStatus: BatteryStatus? = null
) {
    val colors = MaterialTheme.colorScheme
    TopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = title,
                style = TopBarTitleTextStyle,
                color = MaterialTheme.extendedColors.topBarTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = colors.onSurface
                )
            }
        },
        actions = {
            val statusColor = if (isBluetoothConnected) {
                MaterialTheme.extendedColors.connected
            } else {
                colors.onSurfaceVariant
            }

            Surface(
                onClick = onBluetoothClick,
                shape = CircleShape,
                color = colors.surfaceContainerHigh,
                modifier = Modifier.padding(end = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (connectedDeviceName != null) {
                        Text(
                            text = connectedDeviceName,
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 110.dp)
                        )
                    }
                    if (batteryStatus != null) {
                        BatteryIndicator(status = batteryStatus, tint = statusColor)
                    } else {
                        Icon(
                            imageVector = if (isBluetoothConnected) {
                                Icons.Default.BluetoothConnected
                            } else {
                                Icons.Default.BluetoothDisabled
                            },
                            contentDescription = stringResource(R.string.bluetooth_status),
                            tint = statusColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colors.background,
            scrolledContainerColor = colors.surfaceContainer
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
