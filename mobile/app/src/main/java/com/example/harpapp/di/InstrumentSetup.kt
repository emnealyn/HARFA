package com.example.harpapp.di

import com.example.harpapp.domain.repository.InstrumentRepository
import com.example.harpapp.presentation.dev.DevTools

data class InstrumentSetup(
    val repository: InstrumentRepository,
    val devTools: DevTools?
)