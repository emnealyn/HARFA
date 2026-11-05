package com.example.harpapp.testing

import com.example.harpapp.domain.model.ConnectionState
import com.example.harpapp.domain.model.InstrumentCommand
import com.example.harpapp.domain.model.InstrumentDevice
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeInstrumentRepositoryTest {

    @Test
    fun simulatesConnectionAndRecordsCommands() = runBlocking {
        val repository = FakeInstrumentRepository()
        val device = InstrumentDevice(name = "HARFA-1234", address = "00:11:22:33:44:55")

        repository.setDiscoveredDevices(listOf(device))
        repository.scanForDevices()
        assertEquals(listOf(device), repository.discoveredDevices.first())

        repository.connect(device)
        repository.send(InstrumentCommand.SetLeds(mask = 1, mode = 0))

        assertEquals(ConnectionState.CONNECTED, repository.connectionState.first())
        assertEquals(
            listOf(InstrumentCommand.SetLeds(mask = 1, mode = 0)),
            repository.sentCommands
        )
    }

    @Test
    fun emitsStringEventsWithoutAndroidBluetooth() = runBlocking {
        val repository = FakeInstrumentRepository()
        val note = async(start = CoroutineStart.UNDISPATCHED) {
            repository.incomingNotes.first()
        }

        repository.emitStringEvent(stringId = 5, midi = 45, timestampMs = 1000)

        val emittedNote = note.await()
        assertEquals(5, emittedNote.stringId)
        assertEquals(45, emittedNote.midi)
        assertEquals(1000, emittedNote.timestampMs)
    }

    @Test
    fun simulatesConnectionFailureAndDisconnect() = runBlocking {
        val repository = FakeInstrumentRepository()
        val device = InstrumentDevice(name = "HARFA-1234", address = "00:11:22:33:44:55")
        val expected = IllegalStateException("connection failed")
        repository.failNextConnect(expected)

        var actual: Throwable? = null
        try {
            repository.connect(device)
        } catch (exception: Throwable) {
            actual = exception
        }

        assertTrue(actual === expected)
        assertEquals(ConnectionState.DISCONNECTED, repository.connectionState.first())

        repository.simulateConnectionState(ConnectionState.CONNECTED, device.name)
        repository.disconnect()
        assertEquals(ConnectionState.DISCONNECTED, repository.connectionState.first())
    }
}
