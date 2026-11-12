package com.example.harpapp.domain.usecase

import com.example.harpapp.domain.model.BatteryPowerState
import com.example.harpapp.domain.model.BatteryStatus
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class LowBatteryWarningsTest {

    private val low = BatteryStatus(10, 3400, BatteryPowerState.DISCHARGING, isLow = true)
    private val normal = BatteryStatus(50, 3800, BatteryPowerState.DISCHARGING, isLow = false)

    private fun warnings(vararg states: BatteryStatus?): Int = runBlocking {
        flowOf(*states).lowBatteryWarnings().count()
    }

    @Test
    fun ostrzegaRazPrzyUtrzymujacymSieLow() {
        assertEquals(1, warnings(normal, low, low.copy(percent = 9)))
    }

    @Test
    fun ostrzegaPonowniePoZejsciuFlagiLow() {
        assertEquals(2, warnings(low, normal, low))
    }

    @Test
    fun ostrzegaPonowniePoNowymPolaczeniu() {
        assertEquals(2, warnings(low, null, low))
    }

    @Test
    fun nieOstrzegaBezFlagiLow() {
        assertEquals(0, warnings(null, normal, normal.copy(percent = 40), null))
    }
}
