package com.example.harpapp.presentation.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.harpapp.R
import com.example.harpapp.domain.model.NoteName
import com.example.harpapp.presentation.ui.connectionErrorMessage
import com.example.harpapp.presentation.ui.connectionStateMessage
import com.example.harpapp.presentation.ui.connectionStateTitle
import com.example.harpapp.presentation.ui.components.harp.BatterySection
import com.example.harpapp.presentation.ui.components.harp.DeviceCard
import com.example.harpapp.presentation.ui.components.harp.HarpIllustration
import com.example.harpapp.presentation.ui.components.harp.HarpTabs
import com.example.harpapp.presentation.ui.components.harp.LeverButtons
import com.example.harpapp.presentation.ui.components.harp.OctaveShiftSection
import com.example.harpapp.presentation.viewmodel.HarpTab
import com.example.harpapp.presentation.viewmodel.HarpUiState
import com.example.harpapp.presentation.viewmodel.HarpViewModel

@Composable
fun HarpScreen(
    viewModel: HarpViewModel,
    onNavigateToBluetooth: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedNote by viewModel.selectedNote.collectAsState()
    val selectedStrings by viewModel.selectedStrings.collectAsState()
    val tuning = (uiState as? HarpUiState.Connected)?.tuning
    val selectedTab by viewModel.selectedTab.collectAsState()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(0.4f),
                    contentAlignment = Alignment.Center
                ) {
                    HarpIllustration(
                        tuning = tuning,
                        selectedStrings = selectedStrings,
                        modifier = Modifier.fillMaxHeight()
                    )
                }
                HarpContent(
                    uiState = uiState,
                    selectedNote = selectedNote,
                    onNoteClick = viewModel::toggleNote,
                    selectedTab = selectedTab,
                    onTabSelect = viewModel::selectTab,
                    onNavigateToBluetooth = onNavigateToBluetooth,
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HarpIllustration(
                    tuning = tuning,
                    selectedStrings = selectedStrings,
                    modifier = Modifier.height(336.dp)
                )
                HarpContent(
                    uiState = uiState,
                    selectedNote = selectedNote,
                    onNoteClick = viewModel::toggleNote,
                    selectedTab = selectedTab,
                    onTabSelect = viewModel::selectTab,
                    onNavigateToBluetooth = onNavigateToBluetooth,
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun HarpContent(
    uiState: HarpUiState,
    selectedNote: NoteName?,
    onNoteClick: (NoteName) -> Unit,
    selectedTab: HarpTab,
    onTabSelect: (HarpTab) -> Unit,
    onNavigateToBluetooth: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when (uiState) {
            HarpUiState.Disconnected -> DisconnectedCard(onNavigateToBluetooth)
            is HarpUiState.Connecting -> ProgressCard(
                title = stringResource(connectionStateTitle(uiState.state)),
                message = stringResource(connectionStateMessage(uiState.state))
            )
            is HarpUiState.Unavailable -> ConnectionStatusCard(
                title = stringResource(connectionStateTitle(uiState.state)),
                message = stringResource(connectionStateMessage(uiState.state)),
                onNavigateToBluetooth = onNavigateToBluetooth
            )
            is HarpUiState.Error -> ConnectionStatusCard(
                title = stringResource(connectionErrorMessage(uiState.error.code)),
                message = stringResource(R.string.connection_error_message),
                onNavigateToBluetooth = onNavigateToBluetooth
            )
            is HarpUiState.Connected -> {
                HarpTabs(selectedTab, onTabSelect)
                when (selectedTab) {
                    HarpTab.TUNING -> {
                        LeverButtons(
                            tuning = uiState.tuning,
                            selectedNote = selectedNote,
                            onNoteClick = onNoteClick
                        )
                        OctaveShiftSection(uiState.tuning)
                        Text(
                            text = stringResource(R.string.harp_tuning_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HarpTab.DEVICE -> {
                        DeviceCard(uiState.deviceName, uiState.deviceId, onNavigateToBluetooth)
                        BatterySection(uiState.battery)
                    }
                }
            }
        }
    }
}

@Composable
private fun DisconnectedCard(onNavigateToBluetooth: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.harp_disconnected_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.harp_disconnected_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(onClick = onNavigateToBluetooth) {
                Text(stringResource(R.string.harp_connect_button))
            }
        }
    }
}

@Composable
private fun ConnectionStatusCard(
    title: String,
    message: String,
    onNavigateToBluetooth: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(onClick = onNavigateToBluetooth) {
                Text(stringResource(R.string.bluetooth_settings))
            }
        }
    }
}

@Composable
private fun ProgressCard(
    title: String,
    message: String
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
