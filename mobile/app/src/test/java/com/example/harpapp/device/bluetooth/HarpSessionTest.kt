package com.example.harpapp.device.bluetooth

import com.example.harpapp.protocol.DecodeResult
import com.example.harpapp.protocol.HarpCodec
import com.example.harpapp.protocol.HarpMessage
import com.example.harpapp.protocol.HarpProtocol
import com.example.harpapp.protocol.SessionIds
import com.example.harpapp.protocol.StatusCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlinx.coroutines.async
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield

class HarpSessionTest {

    @Test
    fun startsHandshakeAndCompletesAfterDeviceInfo() = runBlocking {
        val sentFrames = mutableListOf<ByteArray>()
        val session = HarpSession(initialSid = 7) { frame -> sentFrames += frame }
        val handshake = async(start = CoroutineStart.UNDISPATCHED) { session.start() }

        val hello = decoded(sentFrames.single())
        assertTrue(hello is HarpMessage.Hello)
        assertEquals(7, hello.sid)
        assertEquals(7, session.currentSid)

        session.handleIncoming(HarpCodec.encode(deviceInfoMessage(sid = 7)))

        assertEquals(1234L, handshake.await().deviceId)
    }

    @Test
    fun everySessionGetsNewNonZeroSid() = runBlocking {
        val sentFrames = mutableListOf<ByteArray>()
        val session = HarpSession(initialSid = SessionIds.MAX) { frame -> sentFrames += frame }

        val sids = List(2) {
            val handshake = async(start = CoroutineStart.UNDISPATCHED) { session.start() }
            val sid = decoded(sentFrames.last()).sid
            session.handleIncoming(HarpCodec.encode(deviceInfoMessage(sid = sid)))
            handshake.await()
            sid
        }

        // Po 255 licznik wraca do 1 i pomija 0 (brak sesji)
        assertEquals(listOf(SessionIds.MAX, SessionIds.MIN), sids)
    }

    @Test
    fun deviceInfoFromOtherSessionDoesNotCompleteHandshake() = runBlocking {
        val session = HarpSession(initialSid = 7) { }
        val handshake = async(start = CoroutineStart.UNDISPATCHED) { session.start() }

        val staleEvent = session.handleIncoming(HarpCodec.encode(deviceInfoMessage(sid = 6)))
        assertTrue(staleEvent is HarpSessionEvent.StaleFrame)
        assertFalse(handshake.isCompleted)

        session.handleIncoming(HarpCodec.encode(deviceInfoMessage(sid = 7)))
        assertEquals(1234L, handshake.await().deviceId)
    }

    @Test
    fun handshakeFailsWhenInstrumentUsesOtherProtocolVersion() = runBlocking {
        val session = HarpSession(initialSid = 7) { }
        val handshake = async(start = CoroutineStart.UNDISPATCHED) { runCatching { session.start() } }

        // ERROR(UNSUPPORTED_VERSION) w formacie 1.0 (spec §9.2)
        val event = session.handleIncoming(byteArrayOf(0x01, 0x11, 0x00, 0x03, 0x01, 0x00, 0x01))

        assertEquals(HarpSessionEvent.InvalidFrame(StatusCode.UNSUPPORTED_VERSION), event)
        assertTrue(handshake.await().exceptionOrNull() is HarpHandshakeException)
        assertEquals(SessionIds.NONE, session.currentSid)
    }

    @Test
    fun exposesStringEvents() = runBlocking {
        val session = connectedSession(mutableListOf(), sid = 7)
        val message = HarpMessage.StringEvent(seq = 2, sid = 7, stringId = 5, midi = 45, tMs = 1000)

        val event = session.handleIncoming(HarpCodec.encode(message))

        assertEquals(HarpSessionEvent.Message(message), event)
    }

    @Test
    fun sendsCommandWithSessionIdAndCompletesAfterMatchingAck() = runBlocking {
        val sentFrames = mutableListOf<ByteArray>()
        val session = connectedSession(sentFrames, sid = 7)

        val command = async(start = CoroutineStart.UNDISPATCHED) {
            session.sendCommand(expectedSid = 7) { sequence, sid ->
                HarpMessage.LedSet(seq = sequence, sid = sid, mask = 1, mode = 0)
            }
        }

        val sentCommand = decoded(sentFrames[1])
        assertTrue(sentCommand is HarpMessage.LedSet)
        assertEquals(1, sentCommand.seq)
        assertEquals(7, sentCommand.sid)

        session.handleIncoming(
            HarpCodec.encode(HarpMessage.Ack(seq = 2, sid = 7, ackSeq = sentCommand.seq, status = 0))
        )
        command.await()
    }

    @Test
    fun commandPreparedInPreviousSessionIsNotSent() = runBlocking {
        val sentFrames = mutableListOf<ByteArray>()
        val session = connectedSession(sentFrames, sid = 7)
        val previousSid = session.currentSid

        // Nowa sesja (ponowne połączenie) przed wysłaniem zakolejkowanej komendy
        val handshake = async(start = CoroutineStart.UNDISPATCHED) { session.start() }
        session.handleIncoming(HarpCodec.encode(deviceInfoMessage(sid = session.currentSid)))
        handshake.await()
        assertNotEquals(previousSid, session.currentSid)
        val framesBefore = sentFrames.size

        try {
            session.sendCommand(expectedSid = previousSid) { sequence, sid ->
                HarpMessage.LessonEnd(seq = sequence, sid = sid)
            }
            fail("Command from previous session should be rejected")
        } catch (expected: HarpStaleSessionException) {
            assertEquals(framesBefore, sentFrames.size)
        }
    }

    @Test
    fun previousGenerationIsRejectedEvenWhenWireSessionIdMatches() = runBlocking {
        val sentFrames = mutableListOf<ByteArray>()
        val session = connectedSession(sentFrames, sid = 7)
        val previousGeneration = session.currentGeneration

        val handshake = async(start = CoroutineStart.UNDISPATCHED) { session.start() }
        val currentSid = session.currentSid
        session.handleIncoming(HarpCodec.encode(deviceInfoMessage(sid = currentSid)))
        handshake.await()

        val framesBefore = sentFrames.size
        try {
            session.sendCommand(
                expectedSid = session.currentSid,
                expectedGeneration = previousGeneration
            ) { sequence, sid ->
                HarpMessage.LessonEnd(seq = sequence, sid = sid)
            }
            fail("Command from previous generation should be rejected")
        } catch (expected: HarpStaleSessionException) {
            assertEquals(framesBefore, sentFrames.size)
        }
    }

    @Test
    fun staleSessionAckRejectsCommand() = runBlocking {
        val sentFrames = mutableListOf<ByteArray>()
        val session = connectedSession(sentFrames, sid = 7)

        val command = async(start = CoroutineStart.UNDISPATCHED) {
            runCatching {
                session.sendCommand(expectedSid = 7) { sequence, sid ->
                    HarpMessage.LessonPause(seq = sequence, sid = sid, paused = 1)
                }
            }
        }
        session.handleIncoming(
            HarpCodec.encode(
                HarpMessage.Ack(seq = 2, sid = 7, ackSeq = 1, status = StatusCode.STALE_SESSION.code)
            )
        )

        assertTrue(command.await().exceptionOrNull() is HarpStaleSessionException)
    }

    @Test
    fun ignoresFramesFromOtherSession() = runBlocking {
        val session = connectedSession(mutableListOf(), sid = 7)
        val staleMessage = HarpMessage.StringEvent(seq = 2, sid = 6, stringId = 5, midi = 45, tMs = 1000)

        val staleEvent = session.handleIncoming(HarpCodec.encode(staleMessage))
        assertEquals(HarpSessionEvent.StaleFrame(staleMessage, currentSid = 7), staleEvent)

        // Odrzucona ramka nie przesuwa licznika seq: kolejna ramka bieżącej sesji nie ma luki
        val nextEvent = session.handleIncoming(
            HarpCodec.encode(HarpMessage.StringEvent(seq = 2, sid = 7, stringId = 5, midi = 45, tMs = 1000))
        )
        assertTrue(nextEvent is HarpSessionEvent.Message)
        assertNull((nextEvent as HarpSessionEvent.Message).sequenceGap)
    }

    @Test
    fun reportsIncomingSequenceGapAfterHandshake() = runBlocking {
        val session = connectedSession(mutableListOf(), sid = 7)

        val gapEvent = session.handleIncoming(
            HarpCodec.encode(HarpMessage.StringEvent(seq = 3, sid = 7, stringId = 5, midi = 45, tMs = 1000))
        )

        assertTrue(gapEvent is HarpSessionEvent.Message)
        assertEquals(
            SequenceGap(expectedSequence = 2, actualSequence = 3),
            (gapEvent as HarpSessionEvent.Message).sequenceGap
        )

        val nextEvent = session.handleIncoming(
            HarpCodec.encode(HarpMessage.StringEvent(seq = 4, sid = 7, stringId = 5, midi = 46, tMs = 1250))
        )

        assertTrue(nextEvent is HarpSessionEvent.Message)
        assertNull((nextEvent as HarpSessionEvent.Message).sequenceGap)
    }

    @Test
    fun resetEndsSession() = runBlocking {
        val session = connectedSession(mutableListOf(), sid = 7)

        session.reset()

        assertEquals(SessionIds.NONE, session.currentSid)
    }

    @Test
    fun idleSessionRejectsAllMessages() {
        val session = HarpSession { }
        val message = HarpMessage.StringEvent(seq = 4, sid = 1, stringId = 5, midi = 45, tMs = 1000)

        val event = session.handleIncoming(HarpCodec.encode(message))

        assertEquals(HarpSessionEvent.StaleFrame(message, SessionIds.NONE), event)
    }

    @Test
    fun handshakeIgnoresNonHandshakeFramesEvenWithCurrentSid() = runBlocking<Unit> {
        val session = HarpSession(initialSid = 7) { }
        val handshake = async(start = CoroutineStart.UNDISPATCHED) { session.start() }
        val message = HarpMessage.StringEvent(seq = 1, sid = 7, stringId = 5, midi = 45, tMs = 1000)

        val event = session.handleIncoming(HarpCodec.encode(message))

        assertEquals(HarpSessionEvent.StaleFrame(message, currentSid = 7), event)
        assertFalse(handshake.isCompleted)

        session.handleIncoming(HarpCodec.encode(deviceInfoMessage(sid = 7)))
        handshake.await()
    }

    @Test
    fun frameRightAfterDeviceInfoIsAccepted() = runBlocking<Unit> {
        val session = HarpSession(initialSid = 7) { }
        val handshake = async { session.start() }
        yield()

        session.handleIncoming(HarpCodec.encode(deviceInfoMessage(sid = 7)))
        val event = session.handleIncoming(
            HarpCodec.encode(HarpMessage.StringEvent(seq = 1, sid = 7, stringId = 5, midi = 45, tMs = 1000))
        )

        assertTrue(event is HarpSessionEvent.Message)
        handshake.await()
    }

    private suspend fun connectedSession(sentFrames: MutableList<ByteArray>, sid: Int): HarpSession =
        kotlinx.coroutines.coroutineScope {
            val session = HarpSession(initialSid = sid) { frame -> sentFrames += frame }
            val handshake = async(start = CoroutineStart.UNDISPATCHED) { session.start() }
            session.handleIncoming(HarpCodec.encode(deviceInfoMessage(sid = sid)))
            handshake.await()
            session
        }

    private fun decoded(frame: ByteArray): HarpMessage {
        val result = HarpCodec.decode(frame)
        assertTrue(result is DecodeResult.Success)
        return (result as DecodeResult.Success).message
    }

    private fun deviceInfoMessage(sid: Int) = HarpMessage.DeviceInfo(
        seq = 1,
        sid = sid,
        protoMajor = HarpProtocol.VERSION_MAJOR,
        protoMinor = HarpProtocol.VERSION_MINOR,
        fwMajor = 1,
        fwMinor = 0,
        fwPatch = 0,
        hwRev = 1,
        stringCount = HarpProtocol.STRING_COUNT,
        caps = 0,
        deviceId = 1234
    )
}
