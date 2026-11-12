package com.example.harpapp.device.bluetooth

import com.example.harpapp.domain.model.InstrumentEvent
import com.example.harpapp.domain.model.InstrumentState
import com.example.harpapp.domain.model.NoteEvent
import com.example.harpapp.protocol.HarpMessage
import com.example.harpapp.domain.model.BatteryStatus
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Zamienia zdekodowane wiadomości harfy na zdarzenia domenowe trybu nauki
 * i przechowuje aktualne strojenie. Dekodowanie ramek i kontrola sekwencji należą do [HarpSession].
 * Nie zależy od Androida.
 */
internal class InstrumentEventStream {

    private val _noteEvents = MutableSharedFlow<NoteEvent>(
        extraBufferCapacity = EVENT_BUFFER_SIZE,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val noteEvents: SharedFlow<NoteEvent> = _noteEvents.asSharedFlow()

    private val _instrumentState = MutableStateFlow(InstrumentState.DEFAULT)
    val instrumentState: StateFlow<InstrumentState> = _instrumentState.asStateFlow()

    private val _batteryStatus = MutableStateFlow<BatteryStatus?>(null)
    val batteryStatus: StateFlow<BatteryStatus?> = _batteryStatus.asStateFlow()

    /**
     * Przyjmuje wiadomość od harfy. Zwraca zdarzenie domenowe ([NoteEvent],
     * [InstrumentEvent.ConfigurationUpdated] albo [InstrumentEvent.BatteryUpdated]) albo `null`,
     * gdy wiadomość nie ma odpowiednika w modelu domenowym lub ma niepoprawne wartości.
     */
    fun accept(message: HarpMessage): InstrumentEvent? = when (message) {
        is HarpMessage.StringEvent -> message.toNoteEvent()?.also { _noteEvents.tryEmit(it) }
        is HarpMessage.ConfigState -> message.toInstrumentState()?.let { state ->
            _instrumentState.value = state
            InstrumentEvent.ConfigurationUpdated(state)
        }
        is HarpMessage.BatteryState -> message.toBatteryStatus()?.let { status ->
            _batteryStatus.value = status
            InstrumentEvent.BatteryUpdated(status)
        }
        else -> null
    }

    /** Po zerwaniu połączenia strojenie jest nieznane do kolejnego handshake (protokół §8). */
    fun reset() {
        _instrumentState.value = InstrumentState.DEFAULT
        _batteryStatus.value = null
    }

    private companion object {
        // Protokół §5.2: harfa buforuje co najmniej 16 ramek, aplikacja przyjmuje z zapasem.
        const val EVENT_BUFFER_SIZE = 64
    }
}
