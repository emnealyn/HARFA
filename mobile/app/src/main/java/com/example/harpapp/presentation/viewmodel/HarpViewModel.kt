package com.example.harpapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.harpapp.domain.model.BatteryStatus
import com.example.harpapp.domain.model.ConnectionError
import com.example.harpapp.domain.model.ConnectionState
import com.example.harpapp.domain.model.HarpLayout
import com.example.harpapp.domain.model.InstrumentState
import com.example.harpapp.domain.model.NoteName
import com.example.harpapp.domain.model.StringId
import com.example.harpapp.domain.model.Tuning
import com.example.harpapp.domain.model.TuningAnalysis
import com.example.harpapp.domain.repository.InstrumentRepository
import com.example.harpapp.domain.model.isOperationInProgress
import com.example.harpapp.domain.usecase.AnalyzeTuningUseCase
import com.example.harpapp.domain.usecase.GetSongTuningUseCase
import com.example.harpapp.domain.usecase.GetSongUseCase
import com.example.harpapp.domain.usecase.GetTuningsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface HarpUiState {
    data object Disconnected : HarpUiState
    data class Connecting(val state: ConnectionState) : HarpUiState
    data class Unavailable(val state: ConnectionState) : HarpUiState
    data class Error(val error: ConnectionError) : HarpUiState
    data class Connected(
        val deviceName: String?,
        val deviceId: Long?,
        val tuning: InstrumentState,
        val battery: BatteryStatus?
    ) : HarpUiState
}

enum class HarpTab {
    TUNING,
    DEVICE
}

data class HarpTarget(
    val songTitle: String,
    val tuningName: String?,
    val analysis: TuningAnalysis?
)

class HarpViewModel(
    instrumentRepository: InstrumentRepository,
    private val getSongUseCase: GetSongUseCase,
    private val getSongTuningUseCase: GetSongTuningUseCase,
    private val analyzeTuningUseCase: AnalyzeTuningUseCase,
    private val getTuningsUseCase: GetTuningsUseCase
) : ViewModel() {

    val detectedTunings: StateFlow<List<Tuning>> = combine(
        flow { emit(getTuningsUseCase()) },
        instrumentRepository.instrumentState,
        instrumentRepository.configurationKnown
    ) { tunings, state, known ->
        if (known) tunings.filter { it.matches(state) } else emptyList()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    private val targetSongId = MutableStateFlow<String?>(null)

    private val targetSong = targetSongId.map { songId ->
        songId?.let { getSongUseCase(it) }?.let { song -> song to getSongTuningUseCase(song) }
    }

    val target: StateFlow<HarpTarget?> = combine(
        targetSong,
        instrumentRepository.instrumentState,
        instrumentRepository.configurationKnown,
        instrumentRepository.connectionState
    ) { songWithTuning, state, configurationKnown, connection ->
        songWithTuning?.let { (song, tuning) ->
            val known = configurationKnown && connection == ConnectionState.CONNECTED
            HarpTarget(
                songTitle = song.title,
                tuningName = tuning?.name,
                analysis = if (known) analyzeTuningUseCase(song, state, tuning) else null
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    fun setTargetSong(songId: String?) {
        val target = songId?.takeIf { it.isNotBlank() }
        targetSongId.value = target
        if (target != null) {
            _selectedTab.value = HarpTab.TUNING
        }
    }

    val uiState: StateFlow<HarpUiState> = combine(
        instrumentRepository.connectionState,
        instrumentRepository.instrumentState,
        instrumentRepository.connectedDeviceName,
        instrumentRepository.connectedDeviceId,
        instrumentRepository.batteryStatus
    ) { connection, tuning, deviceName, deviceId, battery ->
        when {
            connection == ConnectionState.CONNECTED ->
                HarpUiState.Connected(deviceName, deviceId, tuning, battery)
            connection.isOperationInProgress -> HarpUiState.Connecting(connection)
            connection == ConnectionState.BLUETOOTH_DISABLED ||
                connection == ConnectionState.PERMISSION_REQUIRED ->
                HarpUiState.Unavailable(connection)
            connection is ConnectionState.ERROR ->
                HarpUiState.Error(connection.error)
            else -> HarpUiState.Disconnected
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HarpUiState.Disconnected
    )

    private val _selectedNote = MutableStateFlow<NoteName?>(null)
    val selectedNote: StateFlow<NoteName?> = _selectedNote.asStateFlow()

    init {
        viewModelScope.launch {
            instrumentRepository.connectionState
                .filter { it != ConnectionState.CONNECTED }
                .collect { _selectedNote.value = null }
        }
    }

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
