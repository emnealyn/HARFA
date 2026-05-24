package com.example.harpapp.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.harpapp.ui.components.settings.ThemeOptionRow
import com.example.harpapp.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToBluetooth: () -> Unit
) {
    val mode by viewModel.themeMode.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Application Theme",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        ThemeOptionRow(
            selectedMode = mode,
            onModeSelected = { viewModel.setThemeMode(it) },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onNavigateToBluetooth,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Ustawienia Bluetooth")
        }
    }
}