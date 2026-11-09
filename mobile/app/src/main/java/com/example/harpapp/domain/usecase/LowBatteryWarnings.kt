package com.example.harpapp.domain.usecase

import com.example.harpapp.domain.model.BatteryStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

fun Flow<BatteryStatus?>.lowBatteryWarnings(): Flow<Unit> =
    map { status -> status?.isLow == true }
        .distinctUntilChanged()
        .filter { isLow -> isLow }
        .map { }
