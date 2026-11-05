package com.example.harpapp.device.bluetooth

import com.example.harpapp.protocol.HarpCodec
import com.example.harpapp.protocol.HarpMessage
import com.example.harpapp.protocol.HarpProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.async
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.runBlocking

class HarpSessionTest {

    @Test
    fun startsHandshakeAndCompletesAfterDeviceInfo() = runBlocking {
        val sentFrames = mutableListOf<ByteArray>()
        val session = HarpSession { frame -> sentFrames += frame }
        val handshake = async(start = CoroutineStart.UNDISPATCHED) { session.start() }

        val hello = HarpCodec.decode(sentFrames.single()).let { result ->
            assertTrue(result is com.example.harpapp.protocol.DecodeResult.Success)
            (result as com.example.harpapp.protocol.DecodeResult.Success).message
        }
        assertTrue(hello is HarpMessage.Hello)

        session.handleIncoming(
            HarpCodec.encode(
                HarpMessage.DeviceInfo(
                    seq = 1,
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
            )
        )

        assertEquals(1234L, handshake.await().deviceId)
    }

    @Test
    fun exposesStringEvents() {
        val session = HarpSession { }
        val event = session.handleIncoming(
            HarpCodec.encode(
                HarpMessage.StringEvent(
                    seq = 4,
                    stringId = 5,
                    midi = 45,
                    tMs = 1000
                )
            )
        )

        assertEquals(
            HarpSessionEvent.Message(
                HarpMessage.StringEvent(
                    seq = 4,
                    stringId = 5,
                    midi = 45,
                    tMs = 1000
                )
            ),
            event
        )
    }

    @Test
    fun sendsCommandAndCompletesAfterMatchingAck() = runBlocking {
        val sentFrames = mutableListOf<ByteArray>()
        val session = HarpSession { frame -> sentFrames += frame }
        val handshake = async(start = CoroutineStart.UNDISPATCHED) { session.start() }

        session.handleIncoming(HarpCodec.encode(deviceInfoMessage()))
        handshake.await()

        val command = async(start = CoroutineStart.UNDISPATCHED) {
            session.sendCommand { sequence ->
                HarpMessage.LedSet(seq = sequence, mask = 1, mode = 0)
            }
        }

        val sentCommand = HarpCodec.decode(sentFrames[1]).let { result ->
            assertTrue(result is com.example.harpapp.protocol.DecodeResult.Success)
            (result as com.example.harpapp.protocol.DecodeResult.Success).message
        }
        assertTrue(sentCommand is HarpMessage.LedSet)
        assertEquals(1, sentCommand.seq)

        session.handleIncoming(
            HarpCodec.encode(
                HarpMessage.Ack(
                    seq = 2,
                    ackSeq = sentCommand.seq,
                    status = 0
                )
            )
        )
        command.await()
    }

    @Test
    fun reportsIncomingSequenceGapAfterHandshake() = runBlocking {
        val session = HarpSession { }
        val handshake = async(start = CoroutineStart.UNDISPATCHED) { session.start() }

        session.handleIncoming(HarpCodec.encode(deviceInfoMessage()))
        handshake.await()

        val gapEvent = session.handleIncoming(
            HarpCodec.encode(
                HarpMessage.StringEvent(
                    seq = 3,
                    stringId = 5,
                    midi = 45,
                    tMs = 1000
                )
            )
        )

        assertTrue(gapEvent is HarpSessionEvent.Message)
        assertEquals(
            SequenceGap(expectedSequence = 2, actualSequence = 3),
            (gapEvent as HarpSessionEvent.Message).sequenceGap
        )

        val nextEvent = session.handleIncoming(
            HarpCodec.encode(
                HarpMessage.StringEvent(
                    seq = 4,
                    stringId = 5,
                    midi = 46,
                    tMs = 1250
                )
            )
        )

        assertTrue(nextEvent is HarpSessionEvent.Message)
        assertNull((nextEvent as HarpSessionEvent.Message).sequenceGap)
    }

    private fun deviceInfoMessage() = HarpMessage.DeviceInfo(
        seq = 1,
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
