package com.example.harpapp.device.simulator

import com.example.harpapp.domain.model.BatteryPowerState
import com.example.harpapp.domain.model.BatteryStatus
import com.example.harpapp.domain.model.ConnectionState
import com.example.harpapp.domain.model.InstrumentCommand
import com.example.harpapp.domain.model.InstrumentDevice
import com.example.harpapp.domain.model.InstrumentEvent
import com.example.harpapp.domain.model.StaleSessionException
import com.example.harpapp.protocol.BatteryFlags
import com.example.harpapp.protocol.Capabilities
import com.example.harpapp.protocol.LessonResultCode
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
        repository.send(InstrumentCommand.SetLeds(mask = 1, mode = 0), repository.currentSession.value!!)

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
            repository.noteEvents.first()
        }

        repository.emitStringEvent(stringId = 5, midi = 45, timestampMs = 1000)

        val emittedNote = note.await()
        assertEquals(5, emittedNote.stringId.index)
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
        assertTrue(repository.connectionState.first() is ConnectionState.ERROR)

        repository.connect(device)
        repository.disconnect()
        assertEquals(ConnectionState.DISCONNECTED, repository.connectionState.first())
    }

    @Test
    fun scanFindsSimulatorDeviceAndConnectEmitsHandshakeState() = runBlocking {
        val repository = FakeInstrumentRepository()
        val events = async(start = CoroutineStart.UNDISPATCHED) {
            repository.events.take(2).toList()
        }

        repository.scanForDevices()
        val device = repository.discoveredDevices.first().single()
        assertEquals("HARFA-SIM1", device.name)

        repository.connect(device)

        assertEquals(ConnectionState.CONNECTED, repository.connectionState.first())
        assertEquals(0x5131L, repository.connectedDeviceId.first())
        val handshake = events.await()
        assertTrue(handshake[0] is InstrumentEvent.ConfigurationUpdated)
        assertTrue(handshake[1] is InstrumentEvent.BatteryUpdated)
    }

    @Test
    fun exposesConnectionLifecycleStages() = runBlocking {
        val repository = FakeInstrumentRepository(SimulatorConfig(connectDelayMs = 1))
        val states = async(start = CoroutineStart.UNDISPATCHED) {
            repository.connectionState.drop(1).take(4).toList()
        }

        repository.connect(SimulatorConfig().device)

        assertEquals(
            listOf(
                ConnectionState.CONNECTING,
                ConnectionState.DISCOVERING_SERVICES,
                ConnectionState.HANDSHAKING,
                ConnectionState.CONNECTED
            ),
            states.await()
        )
    }

    @Test
    fun rejectsParallelConnectionAttempts() = runBlocking {
        val repository = FakeInstrumentRepository(SimulatorConfig(connectDelayMs = 100))
        val firstConnection = async(start = CoroutineStart.UNDISPATCHED) {
            runCatching { repository.connect(SimulatorConfig().device) }
        }
        delay(10)

        val secondFailure = runCatching {
            repository.connect(SimulatorConfig().device)
        }.exceptionOrNull()

        assertTrue(secondFailure is IllegalStateException)
        repository.disconnect()
        firstConnection.cancelAndJoin()
    }

    @Test
    fun pluckUsesCurrentTuningAndRequiresConnection() = runBlocking {
        val repository = FakeInstrumentRepository()
        assertFalse(repository.pluck(0))

        repository.reconnect()
        val note = async(start = CoroutineStart.UNDISPATCHED) { repository.noteEvents.first() }
        assertTrue(repository.pluck(0))

        assertEquals(36, note.await().midi) // struna 0 = C2 w strojeniu naturalnym
    }

    @Test
    fun lessonCommandsDriveLedsAndConnectionLossClearsThem() = runBlocking {
        val repository = FakeInstrumentRepository()
        repository.reconnect()

        repository.send(InstrumentCommand.StartLesson(totalSteps = 2), repository.currentSession.value!!)
        repository.send(InstrumentCommand.LessonStep(index = 0, mask = 1 shl 11), repository.currentSession.value!!)
        assertEquals(1 shl 11, repository.litStrings.value)

        val note = async(start = CoroutineStart.UNDISPATCHED) { repository.noteEvents.first() }
        assertTrue(repository.pluckLitString())
        assertEquals(11, note.await().stringId.index)

        repository.simulateConnectionLoss()
        assertEquals(0, repository.litStrings.value)
        assertEquals(ConnectionState.DISCONNECTED, repository.connectionState.first())

        repository.reconnect()
        assertEquals(ConnectionState.CONNECTED, repository.connectionState.first())
    }

    @Test
    fun pauseKeepsCurrentStepAndResumeRelightsIt() = runBlocking {
        val repository = FakeInstrumentRepository()
        repository.reconnect()
        val session = repository.currentSession.value!!
        repository.send(InstrumentCommand.StartLesson(totalSteps = 2), session)
        repository.send(InstrumentCommand.LessonStep(index = 0, mask = 1 shl 11), session)

        repository.send(InstrumentCommand.PauseLesson(paused = true), session)
        assertEquals(0, repository.litStrings.value)

        // Krok wysłany w pauzie jest zapamiętany, ale diody zapalają się dopiero po wznowieniu
        repository.send(InstrumentCommand.LessonStep(index = 1, mask = 1 shl 3), session)
        assertEquals(0, repository.litStrings.value)

        repository.send(InstrumentCommand.PauseLesson(paused = false), session)
        assertEquals(1 shl 3, repository.litStrings.value)
    }

    @Test
    fun everyConnectionIsNewSessionAndCommandsFromOldSessionAreRejected() = runBlocking {
        val repository = FakeInstrumentRepository()
        repository.reconnect()
        val oldSession = repository.currentSession.value!!

        repository.simulateConnectionLoss()
        assertNull(repository.currentSession.value)
        repository.reconnect()
        assertNotEquals(oldSession, repository.currentSession.value)

        var error: Throwable? = null
        try {
            repository.send(InstrumentCommand.LessonStep(index = 0, mask = 1), oldSession)
        } catch (e: StaleSessionException) {
            error = e
        }
        assertTrue(error != null)
        assertTrue(repository.sentCommands.isEmpty())
        assertEquals(0, repository.litStrings.value)
    }

    @Test
    fun rejectsCommandsWithoutCapability() = runBlocking {
        val repository = FakeInstrumentRepository(SimulatorConfig(capabilities = 0))
        repository.reconnect()

        var error: Throwable? = null
        try {
            repository.send(InstrumentCommand.LessonResult(LessonResultCode.HIT), repository.currentSession.value!!)
        } catch (e: IllegalArgumentException) {
            error = e
        }
        assertTrue(error != null)
        assertTrue(repository.sentCommands.isEmpty())
    }

    @Test
    fun scenarioIsDeterministic() = runBlocking {
        val repository = FakeInstrumentRepository()
        val notes = async(start = CoroutineStart.UNDISPATCHED) {
            repository.noteEvents.take(2).toList()
        }

        repository.runScenario(
            listOf(
                SimulatorStep.Scan,
                SimulatorStep.Connect,
                SimulatorStep.Pluck(3),
                SimulatorStep.LoseConnection,
                SimulatorStep.Pluck(4), // zignorowane: brak połączenia
                SimulatorStep.Reconnect,
                SimulatorStep.Pluck(5)
            )
        )

        assertEquals(listOf(3, 5), notes.await().map { it.stringId.index })
        assertEquals(listOf(250, 500), notes.await().map { it.timestampMs })
    }

    @Test
    fun batteryScenarioUpdatesStatus() = runBlocking {
        val repository = FakeInstrumentRepository()

        repository.runScenario(
            listOf(SimulatorStep.Connect, SimulatorStep.Battery(10, 3400, BatteryFlags.LOW))
        )

        assertEquals(
            BatteryStatus(10, 3400, BatteryPowerState.DISCHARGING, isLow = true),
            repository.batteryStatus.first()
        )
    }

    @Test
    fun batteryStatusIsHiddenWithoutConnection() = runBlocking {
        val repository = FakeInstrumentRepository()
        assertNull(repository.batteryStatus.first())

        repository.reconnect()
        assertNotNull(repository.batteryStatus.first())

        repository.disconnect()
        assertNull(repository.batteryStatus.first())

        repository.reconnect()
        repository.simulateConnectionLoss()
        assertNull(repository.batteryStatus.first())
    }

    @Test
    fun batteryStatusIsHiddenWithoutCapability() = runBlocking {
        val repository = FakeInstrumentRepository(
            SimulatorConfig(capabilities = Capabilities.LEDS or Capabilities.LESSON)
        )

        repository.reconnect()
        repository.emitBatteryState(percent = 10, millivolts = 3400, flags = BatteryFlags.LOW)

        assertNull(repository.batteryStatus.first())
    }
}
