package com.example.harpapp.ui.components.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.harpapp.R
import com.example.harpapp.data.ThemeMode
import com.example.harpapp.ui.theme.HARPAppTheme

@Composable
fun ThemeOptionRow(
    selectedMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = selectedMode == ThemeMode.DARK

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (isDark) stringResource(R.string.dark_mode) else stringResource(R.string.light_mode),
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Switch(
            checked = isDark,
            onCheckedChange = { enabled ->
                onModeSelected(if (enabled) ThemeMode.DARK else ThemeMode.LIGHT)
            }
        )
    }
}

@Preview(showBackground = true, name = "Light mode")
@Composable
fun ThemeOptionRowLightPreview() {
    HARPAppTheme {
        ThemeOptionRow(
            selectedMode = ThemeMode.LIGHT,
            onModeSelected = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, name = "Dark mode")
@Composable
fun ThemeOptionRowDarkPreview() {
    HARPAppTheme {
        ThemeOptionRow(
            selectedMode = ThemeMode.DARK,
            onModeSelected = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
