package com.example.harpapp.presentation.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.harpapp.R
import com.example.harpapp.domain.model.NoteName
import com.example.harpapp.domain.model.Tuning
import com.example.harpapp.domain.model.TuningAnalysis
import com.example.harpapp.presentation.ui.components.harp.BatterySection
import com.example.harpapp.presentation.ui.components.harp.DeviceCard
import com.example.harpapp.presentation.ui.components.harp.HarpIllustration
import com.example.harpapp.presentation.ui.components.harp.HarpTabs
import com.example.harpapp.presentation.ui.components.harp.LeverButtons
import com.example.harpapp.presentation.ui.components.harp.OctaveShiftSection
import com.example.harpapp.presentation.ui.components.song.tuningDetails
import com.example.harpapp.presentation.ui.components.song.tuningTitle
import com.example.harpapp.presentation.ui.theme.ConnectedGreen
import com.example.harpapp.presentation.viewmodel.HarpTarget
import com.example.harpapp.presentation.viewmodel.HarpTab
import com.example.harpapp.presentation.viewmodel.HarpUiState
import com.example.harpapp.presentation.viewmodel.HarpViewModel

@Composable
fun HarpScreen(
    viewModel: HarpViewModel,
    onNavigateToBluetooth: () -> Unit,
    onBackToSong: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedNote by viewModel.selectedNote.collectAsState()
    val selectedStrings by viewModel.selectedStrings.collectAsState()
    val tuning = (uiState as? HarpUiState.Connected)?.tuning
    val selectedTab by viewModel.selectedTab.collectAsState()
    val target by viewModel.target.collectAsState()
    val detectedTunings by viewModel.detectedTunings.collectAsState()
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
                    target = target,
                    detectedTunings = detectedTunings,
                    onNavigateToBluetooth = onNavigateToBluetooth,
                    onBackToSong = onBackToSong,
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
                    target = target,
                    detectedTunings = detectedTunings,
                    onNavigateToBluetooth = onNavigateToBluetooth,
                    onBackToSong = onBackToSong,
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
    target: HarpTarget?,
    detectedTunings: List<Tuning>,
    onNavigateToBluetooth: () -> Unit,
    onBackToSong: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when (uiState) {
            HarpUiState.Disconnected -> DisconnectedCard(onNavigateToBluetooth)
            HarpUiState.Connecting -> ProgressCard(stringResource(R.string.harp_connecting))
            is HarpUiState.Connected -> {
                HarpTabs(selectedTab, onTabSelect)
                when (selectedTab) {
                    HarpTab.TUNING -> {
                        val changes = target?.analysis as? TuningAnalysis.NeedsChanges
                        target?.let { TargetCard(it, onBackToSong) }
                        LeverButtons(
                            tuning = uiState.tuning,
                            selectedNote = selectedNote,
                            onNoteClick = onNoteClick,
                            highlight = changes?.levers.orEmpty().associate { it.name to it.to }
                        )
                        if (detectedTunings.isNotEmpty()) {
                            Text(
                                text = stringResource(
                                    R.string.harp_detected_key,
                                    detectedTunings.joinToString(" / ") { it.name }
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        OctaveShiftSection(uiState.tuning, targetShift = changes?.octave)
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
private fun TargetCard(target: HarpTarget, onBackToSong: () -> Unit) {
    val analysis = target.analysis
    val ready = analysis == TuningAnalysis.Compatible
    val containerColor by animateColorAsState(
        targetValue = if (ready) ConnectedGreen.copy(alpha = 0.1f) else MaterialTheme.colorScheme.errorContainer,
        label = "targetCardColor"
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = if (ready) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onErrorContainer
        ),
        border = if (ready) BorderStroke(1.dp, ConnectedGreen.copy(alpha = 0.2f)) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.harp_target_title, target.songTitle),
                style = MaterialTheme.typography.titleMedium
            )
            target.tuningName?.let { name ->
                Text(
                    text = stringResource(R.string.song_key, name),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (analysis == null) {
                Text(
                    text = stringResource(R.string.harp_target_unknown),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    text = tuningTitle(analysis),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (ready) ConnectedGreen else Color.Unspecified
                )
                tuningDetails(analysis).forEach { line ->
                    Text(text = line, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (ready) {
                TextButton(
                    onClick = onBackToSong,
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = ConnectedGreen)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.harp_back_to_song))
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
private fun ProgressCard(message: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
