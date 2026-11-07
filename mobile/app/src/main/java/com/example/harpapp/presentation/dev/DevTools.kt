package com.example.harpapp.presentation.dev

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Narzędzia deweloperskie dostępne tylko w buildzie debug. W release zawsze null. */
interface DevTools {
    @Composable
    fun SettingsEntry(onOpenSimulatorPanel: () -> Unit)

    @Composable
    fun SimulatorPanel()

    @Composable
    fun FloatingStrings(modifier: Modifier)
}