package com.example.harpapp.presentation.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BatteryUnknown
import androidx.compose.material.icons.filled.Battery1Bar
import androidx.compose.material.icons.filled.Battery2Bar
import androidx.compose.material.icons.filled.Battery3Bar
import androidx.compose.material.icons.filled.Battery4Bar
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Power
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.harpapp.R
import com.example.harpapp.domain.model.BatteryPowerState
import com.example.harpapp.domain.model.BatteryStatus

@Composable
fun BatteryIndicator(
    status: BatteryStatus,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Icon(
        imageVector = batteryIcon(status),
        contentDescription = batteryDescription(status),
        tint = tint,
        modifier = modifier
            .size(24.dp)
            .rotate(if (status.powerState == BatteryPowerState.EXTERNAL_POWER) 0f else 90f)
    )
}

@Composable
private fun batteryDescription(status: BatteryStatus): String {
    val level = status.percent?.let { percent ->
        stringResource(R.string.battery_level_percent, percent)
    } ?: stringResource(R.string.battery_level_unknown)
    val powerState = when (status.powerState) {
        BatteryPowerState.DISCHARGING -> stringResource(R.string.battery_state_discharging)
        BatteryPowerState.CHARGING -> stringResource(R.string.battery_state_charging)
        BatteryPowerState.EXTERNAL_POWER -> stringResource(R.string.battery_state_external_power)
    }
    val low = if (status.isLow) stringResource(R.string.battery_state_low) else null
    return stringResource(
        R.string.battery_description,
        listOfNotNull(level, powerState, low).joinToString(", ")
    )
}

private fun batteryIcon(status: BatteryStatus): ImageVector {
    val percent = status.percent
    return when {
        status.powerState == BatteryPowerState.CHARGING -> Icons.Filled.BatteryChargingFull
        status.powerState == BatteryPowerState.EXTERNAL_POWER -> Icons.Filled.Power
        status.isLow -> Icons.Filled.BatteryAlert
        percent == null -> Icons.AutoMirrored.Filled.BatteryUnknown
        percent >= 90 -> Icons.Filled.BatteryFull
        percent >= 75 -> Icons.Filled.Battery5Bar
        percent >= 60 -> Icons.Filled.Battery4Bar
        percent >= 45 -> Icons.Filled.Battery3Bar
        percent >= 30 -> Icons.Filled.Battery2Bar
        else -> Icons.Filled.Battery1Bar
    }
}
