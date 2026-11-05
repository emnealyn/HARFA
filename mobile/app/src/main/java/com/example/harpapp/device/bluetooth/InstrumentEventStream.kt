package com.example.harpapp.device.bluetooth

import com.example.harpapp.domain.model.InstrumentState
import com.example.harpapp.domain.model.NoteEvent
import com.example.harpapp.protocol.DecodeResult
import com.example.harpapp.protocol.HarpCodec
import com.example.harpapp.protocol.HarpMessage
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Zamienia ramki protokołu odebrane z harfy na zdarzenia domenowe.
 * Nie zależy od Androida ani od transportu: transport (BLE) podaje tu kompletne ramki.
 *
 * Niepoprawne ramki oraz wiadomości nieistotne dla trybu nauki są pomijane
 * (docs/protocol/ble-protocol.md §6.2).
 */
class InstrumentEventStream {

    private val _noteEvents = MutableSharedFlow<NoteEvent>(
        extraBufferCapacity = EVENT_BUFFER_SIZE,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val noteEvents: SharedFlow<NoteEvent> = _noteEvents.asSharedFlow()

    private val _instrumentState = MutableStateFlow(InstrumentState.DEFAULT)
    val instrumentState: StateFlow<InstrumentState> = _instrumentState.asStateFlow()

    /** Przyjmuje jedną ramkę. Zwraca `true`, gdy ramka zmieniła stan domenowy. */
    fun accept(frame: ByteArray): Boolean {
        val result = HarpCodec.decode(frame)
        return result is DecodeResult.Success && accept(result.message)
    }

    fun accept(message: HarpMessage): Boolean = when (message) {
        is HarpMessage.StringEvent -> message.toNoteEvent()?.let { _noteEvents.tryEmit(it) } ?: false
        is HarpMessage.ConfigState -> message.toInstrumentState()?.let {
            _instrumentState.value = it
            true
        } ?: false
        else -> false
    }

    /** Po zerwaniu połączenia strojenie jest nieznane do kolejnego handshake (protokół §8). */
    fun reset() {
        _instrumentState.value = InstrumentState.DEFAULT
    }

    private companion object {
        // Protokół §5.2: harfa buforuje co najmniej 16 ramek, aplikacja przyjmuje z zapasem.
        const val EVENT_BUFFER_SIZE = 64
    }
}
