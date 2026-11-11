package com.example.harpapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.harpapp.domain.model.ConnectionState
import com.example.harpapp.domain.model.HarpLayout
import com.example.harpapp.domain.model.InstrumentState
import com.example.harpapp.domain.model.NoteName
import com.example.harpapp.domain.model.StringId
import com.example.harpapp.domain.repository.InstrumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface HarpUiState {
    data object Disconnected : HarpUiState
    data object Connecting : HarpUiState
    data class Connected(
        val deviceName: String?,
        val deviceId: Long?,
        val tuning: InstrumentState
    ) : HarpUiState
}

enum class HarpTab {
    TUNING,
    DEVICE
}

class HarpViewModel(
    instrumentRepository: InstrumentRepository
) : ViewModel() {

    val uiState: StateFlow<HarpUiState> = combine(
        instrumentRepository.connectionState,
        instrumentRepository.instrumentState,
        instrumentRepository.connectedDeviceName,
        instrumentRepository.connectedDeviceId
    ) { connection, tuning, deviceName, deviceId ->
        when (connection) {
            ConnectionState.DISCONNECTED -> HarpUiState.Disconnected
            ConnectionState.CONNECTING -> HarpUiState.Connecting
            ConnectionState.CONNECTED -> HarpUiState.Connected(deviceName, deviceId, tuning)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HarpUiState.Disconnected
    )

    private val _selectedNote = MutableStateFlow<NoteName?>(null)
    val selectedNote: StateFlow<NoteName?> = _selectedNote.asStateFlow()

    val selectedStrings: StateFlow<List<StringId>> = _selectedNote
        .map { name -> name?.let(HarpLayout::stringsOf).orEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _selectedTab = MutableStateFlow(HarpTab.TUNING)
    val selectedTab: StateFlow<HarpTab> = _selectedTab.asStateFlow()

    fun toggleNote(name: NoteName) {
        _selectedNote.value = if (_selectedNote.value == name) null else name
    }

    fun selectTab(tab: HarpTab) {
        _selectedTab.value = tab
    }
}
