package com.example.harpapp.di

import android.content.Context
import com.example.harpapp.device.bluetooth.BluetoothInstrumentRepository

/** Release: zawsze prawdziwa harfa, bez symulatora i narzędzi deweloperskich. */
object InstrumentModule {
    fun create(context: Context): InstrumentSetup =
        InstrumentSetup(
            repository = BluetoothInstrumentRepository(context),
            devTools = null
        )
}