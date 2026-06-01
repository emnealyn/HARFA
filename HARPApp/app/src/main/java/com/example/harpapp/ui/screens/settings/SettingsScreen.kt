package com.example.harpapp.ui.screens.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.example.harpapp.R
import com.example.harpapp.ui.components.settings.ThemeOptionRow
import com.example.harpapp.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToBluetooth: () -> Unit
) {
    val mode by viewModel.themeMode.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    val currentLocale = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    val isPolish = currentLocale.startsWith("pl")
    val selectedLanguageLabel = if (isPolish) "PL" else "EN"

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = stringResource(R.string.application_theme),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp),
            color = MaterialTheme.colorScheme.onBackground
        )

        ThemeOptionRow(
            selectedMode = mode,
            onModeSelected = { viewModel.setThemeMode(it) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.settings_language_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = selectedLanguageLabel,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .width(100.dp)
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true),
                    shape = MaterialTheme.shapes.small,
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("EN") },
                        onClick = {
                            viewModel.changeAppLanguage("en")
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("PL") },
                        onClick = {
                            viewModel.changeAppLanguage("pl")
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onNavigateToBluetooth,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.background
            )
        ) {
            Text(stringResource(R.string.bluetooth_settings))
        }
    }
}

private fun changeAppLanguage(languageCode: String) {
    val appLocale = LocaleListCompat.forLanguageTags(languageCode)
    AppCompatDelegate.setApplicationLocales(appLocale)
}