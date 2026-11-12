package com.example.harpapp.presentation.ui.components.harp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.harpapp.R
import com.example.harpapp.domain.model.BatteryPowerState
import com.example.harpapp.domain.model.BatteryStatus
import com.example.harpapp.presentation.ui.components.BatteryIndicator

@Composable
fun BatterySection(battery: BatteryStatus?) {
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
