package com.example.harpapp.presentation.dev

import android.content.Context

class SimulatorPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    val isSimulatorEnabled: Boolean get() = prefs.getBoolean(KEY_ENABLED, false)
    fun setSimulatorEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).commit()
    }

    val isFloatingStringsEnabled: Boolean get() = prefs.getBoolean(KEY_FLOATING_STRINGS, true)
    fun setFloatingStringsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FLOATING_STRINGS, enabled).apply()
    }

    private companion object {
        const val PREFS = "dev_prefs"
        const val KEY_ENABLED = "simulator_enabled"
        const val KEY_FLOATING_STRINGS = "floating_strings_enabled"
    }
}