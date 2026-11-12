package com.example.harpapp.presentation.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.harpapp.R
import com.example.harpapp.domain.model.Accidental
import com.example.harpapp.domain.model.BatteryPowerState
import com.example.harpapp.domain.model.BatteryStatus
import com.example.harpapp.domain.model.InstrumentState
import com.example.harpapp.domain.model.NoteName
import com.example.harpapp.domain.model.OctaveShift
import com.example.harpapp.domain.model.Pitch
import com.example.harpapp.presentation.ui.components.BatteryIndicator
import com.example.harpapp.presentation.ui.components.HarpIllustration
import com.example.harpapp.presentation.ui.components.tuningColor
import com.example.harpapp.presentation.viewmodel.HarpTab
import com.example.harpapp.presentation.viewmodel.HarpUiState
import com.example.harpapp.presentation.viewmodel.HarpViewModel
import kotlin.math.roundToInt

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
            HarpUiState.Connecting -> ProgressCard(stringResource(R.string.harp_connecting))
            is HarpUiState.Connected -> {
                HarpTabs(selectedTab, onTabSelect)
                when (selectedTab) {
                    HarpTab.TUNING -> {
                        LeverButtons(
                            tuning = uiState.tuning,
                            selectedNote = selectedNote,
                            onNoteClick = onNoteClick
                        )
                        TuningCard(uiState.tuning)
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
private fun HarpTabs(selectedTab: HarpTab, onTabSelect: (HarpTab) -> Unit) {
    val lineColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val stroke = 2.dp.toPx()
                drawLine(
                    color = lineColor,
                    start = Offset(0f, size.height - stroke / 2),
                    end = Offset(size.width, size.height - stroke / 2),
                    strokeWidth = stroke
                )
            },
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Bottom
    ) {
        HarpTab.entries.forEach { tab ->
            BookTab(
                title = stringResource(tabTitle(tab)),
                selected = tab == selectedTab,
                onClick = { onTabSelect(tab) }
            )
        }
    }
}

@Composable
private fun BookTab(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .widthIn(min = 132.dp)
            .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
            .background(if (selected) colors.primary else colors.secondary.copy(alpha = 0.15f))
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
            .padding(horizontal = 16.dp, vertical = if (selected) 8.dp else 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) colors.onPrimary else colors.secondary
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
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

@Composable
private fun LeverButtons(
    tuning: InstrumentState,
    selectedNote: NoteName?,
    onNoteClick: (NoteName) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        NoteName.entries.forEach { name ->
            LeverButton(
                name = name,
                position = tuning.lever(name),
                selected = name == selectedNote,
                onClick = { onNoteClick(name) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun LeverButton(
    name: NoteName,
    position: Accidental,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val tuningBackground by animateColorAsState(
        targetValue = tuningColor(position).copy(alpha = 0.2f),
        label = "leverBackground"
    )

    Surface(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) colors.primary else tuningBackground,
        contentColor = if (selected) colors.onPrimary else colors.primary,
        border = BorderStroke(1.dp, colors.primary)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = name.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = accidentalSymbol(position),
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TuningCard(tuning: InstrumentState) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.harp_octave_shift_title))
        OctaveShiftSlider(
            current = tuning.octaveShift,
            baseOctave = tuning.baseOctave
        )
    }
}

@Composable
private fun OctaveShiftSlider(current: OctaveShift, baseOctave: Int, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val stops = OctaveShift.entries
    val thumbPosition by animateFloatAsState(
        targetValue = stops.indexOf(current).toFloat(),
        label = "octaveShiftThumb"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val step = size.width / stops.size
                val start = step / 2
                val centerY = size.height / 2
                fun stopX(position: Float) = start + position * step

                drawLine(
                    color = colors.secondary,
                    start = Offset(stopX(0f), centerY),
                    end = Offset(stopX(stops.lastIndex.toFloat()), centerY),
                    strokeWidth = 12.dp.toPx(),
                    cap = StrokeCap.Round
                )
                stops.indices.forEach { index ->
                    drawCircle(
                        color = colors.onSecondary,
                        radius = 3.dp.toPx(),
                        center = Offset(stopX(index.toFloat()), centerY)
                    )
                }
                drawCircle(
                    color = colors.primary,
                    radius = 15.dp.toPx(),
                    center = Offset(stopX(thumbPosition), centerY)
                )
            }
            AlignedToStop(
                position = thumbPosition,
                stopCount = stops.size,
                modifier = Modifier.matchParentSize()
            ) {
                Text(
                    text = octaveShiftLabel(current),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onPrimary
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            stops.forEach { shift ->
                val active = shift == current
                val naturalTuning = InstrumentState(octaveShift = shift, baseOctave = baseOctave)
                Text(
                    text = stringResource(
                        R.string.harp_range,
                        pitchLabel(naturalTuning.lowestPitch),
                        pitchLabel(naturalTuning.highestPitch)
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    color = if (active) colors.primary else colors.secondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AlignedToStop(
    position: Float,
    stopCount: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val width = constraints.maxWidth
        val placeable = measurables.single().measure(constraints.copy(minWidth = 0, minHeight = 0))
        val height = maxOf(placeable.height, constraints.minHeight)
        val step = width / stopCount.toFloat()
        val center = step / 2 + position * step
        val x = (center - placeable.width / 2f).roundToInt().coerceIn(0, maxOf(0, width - placeable.width))
        layout(width, height) {
            placeable.place(x, (height - placeable.height) / 2)
        }
    }
}

@Composable
private fun DeviceCard(deviceName: String?, deviceId: Long?, onNavigateToBluetooth: () -> Unit) {
    Card(
        onClick = onNavigateToBluetooth,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.BluetoothConnected,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Column {
                Text(
                    text = stringResource(R.string.harp_device_title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = deviceName ?: stringResource(R.string.unknown_device),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (deviceId != null) {
                    Text(
                        text = stringResource(R.string.harp_device_id, "0x" + deviceId.toString(16).uppercase()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun BatterySection(battery: BatteryStatus?) {
    val colors = MaterialTheme.colorScheme
    val unavailable = stringResource(R.string.harp_value_unavailable)
    val level = battery?.percent?.let { stringResource(R.string.battery_level_percent, it) } ?: unavailable
    val voltage = battery?.millivolts?.let { stringResource(R.string.harp_battery_voltage_value, it / 1000.0) }
        ?: unavailable
    val power = when (battery?.powerState) {
        null -> unavailable
        BatteryPowerState.DISCHARGING -> stringResource(R.string.harp_battery_power_battery)
        BatteryPowerState.CHARGING -> stringResource(R.string.harp_battery_charging)
        BatteryPowerState.EXTERNAL_POWER -> stringResource(R.string.harp_battery_external_power)
    }
    val low = when (battery?.isLow) {
        null -> unavailable
        true -> stringResource(R.string.harp_yes)
        false -> stringResource(R.string.harp_no)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.harp_battery_title))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoRow(label = stringResource(R.string.harp_battery_level), value = level) {
                    if (battery != null) {
                        BatteryIndicator(status = battery, tint = if (battery.isLow) colors.error else colors.primary)
                    }
                }
                InfoRow(label = stringResource(R.string.harp_battery_voltage), value = voltage)
                InfoRow(label = stringResource(R.string.harp_battery_power), value = power)
                if (battery?.isLow == true) {
                    LowBatteryRow(value = low)
                } else {
                    InfoRow(label = stringResource(R.string.harp_battery_low), value = low)
                }
            }
        }
    }
}

@Composable
private fun LowBatteryRow(value: String) {
    val colors = MaterialTheme.colorScheme
    var expanded by rememberSaveable { mutableStateOf(false) }

    Surface(
        onClick = { expanded = !expanded },
        modifier = Modifier
            .layout { measurable, constraints ->
                val bleed = 8.dp.roundToPx()
                val placeable = measurable.measure(
                    constraints.copy(
                        minWidth = constraints.maxWidth + 2 * bleed,
                        maxWidth = constraints.maxWidth + 2 * bleed
                    )
                )
                layout(constraints.maxWidth, placeable.height) {
                    placeable.place(-bleed, 0)
                }
            },
        shape = MaterialTheme.shapes.small,
        color = colors.errorContainer,
        contentColor = colors.onErrorContainer
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.harp_battery_low),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }
            AnimatedVisibility(visible = expanded) {
                Text(
                    text = stringResource(R.string.harp_battery_low_hint),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    icon: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        icon()
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun tabTitle(tab: HarpTab): Int = when (tab) {
    HarpTab.TUNING -> R.string.harp_tab_tuning
    HarpTab.DEVICE -> R.string.harp_tab_device
}

private fun octaveShiftLabel(shift: OctaveShift): String = when (shift) {
    OctaveShift.DOWN -> "−1"
    OctaveShift.NONE -> "0"
    OctaveShift.UP -> "+1"
}

private fun accidentalSymbol(accidental: Accidental): String = when (accidental) {
    Accidental.FLAT -> "♭"
    Accidental.NATURAL -> "♮"
    Accidental.SHARP -> "♯"
}

private fun pitchLabel(pitch: Pitch): String {
    val accidental = if (pitch.accidental == Accidental.NATURAL) "" else accidentalSymbol(pitch.accidental)
    return pitch.name.name + accidental + pitch.octave
}
