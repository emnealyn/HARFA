package com.example.harpapp.device.bluetooth

import com.example.harpapp.protocol.DecodeResult
import com.example.harpapp.protocol.HarpCodec
import com.example.harpapp.protocol.HarpMessage
import com.example.harpapp.protocol.HarpProtocol
import com.example.harpapp.protocol.SessionIds
import com.example.harpapp.protocol.StatusCode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.io.IOException
import kotlin.random.Random

internal sealed interface HarpSessionEvent {
    data class Message(
        val message: HarpMessage,
        val sequenceGap: SequenceGap? = null
    ) : HarpSessionEvent

    data class InvalidFrame(val status: StatusCode) : HarpSessionEvent
    data class StaleFrame(val message: HarpMessage, val currentSid: Int) : HarpSessionEvent
}

internal data class SequenceGap(
    val expectedSequence: Int,
    val actualSequence: Int
)

internal class HarpSession(
    initialSid: Int = Random.nextInt(SessionIds.MIN, SessionIds.MAX + 1),
    private val sendFrame: suspend (ByteArray) -> Unit
) {

    private var state = State.IDLE
    private var deviceInfoWaiter: CompletableDeferred<HarpMessage.DeviceInfo>? = null
    private var pendingAck: PendingAck? = null
    private var nextOutgoingSequence = 0
    private var lastIncomingSequence: Int? = null
    private val commandMutex = Mutex()

    private var nextSid = initialSid.also {
        require(it in SessionIds.MIN..SessionIds.MAX) { "Invalid initial sid $it" }
    }

    var currentSid: Int = SessionIds.NONE
        private set

    suspend fun start(): HarpMessage.DeviceInfo {
        reset()
        state = State.HANDSHAKING
        currentSid = allocateSid()

        repeat(MAX_HELLO_ATTEMPTS) { attempt ->
            val waiter = CompletableDeferred<HarpMessage.DeviceInfo>()
            deviceInfoWaiter = waiter

            try {
                sendFrame(
                    HarpCodec.encode(
                        HarpMessage.Hello(
                            seq = HELLO_SEQUENCE,
                            sid = currentSid,
                            protoMajor = HarpProtocol.VERSION_MAJOR,
                            protoMinor = HarpProtocol.VERSION_MINOR
                        )
                    )
                )

                val deviceInfo = withTimeout(HANDSHAKE_TIMEOUT_MS) {
                    waiter.await()
                }
                deviceInfoWaiter = null
                return deviceInfo
            } catch (exception: TimeoutCancellationException) {
                deviceInfoWaiter = null
                if (attempt == MAX_HELLO_ATTEMPTS - 1) {
                    endSession()
                    throw HarpHandshakeException(
                        "No DEVICE_INFO received after $MAX_HELLO_ATTEMPTS HELLO attempts"
                    )
                }
            } catch (exception: HarpHandshakeException) {
                deviceInfoWaiter = null
                endSession()
                throw exception
            } catch (exception: CancellationException) {
                deviceInfoWaiter = null
                endSession()
                throw exception
            } catch (exception: IOException) {
                deviceInfoWaiter = null
                endSession()
                throw exception
            }
        }

        error("Handshake did not complete")
    }

    suspend fun sendCommand(expectedSid: Int, messageFactory: (seq: Int, sid: Int) -> HarpMessage) {
        commandMutex.withLock {
            check(state == State.READY) { "HARFA session is not ready" }
            if (expectedSid != currentSid) {
                throw HarpStaleSessionException(
                    "Command prepared for session $expectedSid, current session is $currentSid"
                )
            }

            val sequence = nextOutgoingSequence
            nextOutgoingSequence = (nextOutgoingSequence + 1) and 0xFF
            val message = messageFactory(sequence, currentSid)
            val frame = HarpCodec.encode(message)

            repeat(MAX_COMMAND_ATTEMPTS) { attempt ->
                val waiter = CompletableDeferred<Int>()
                pendingAck = PendingAck(
                    sequence = sequence,
                    typeCode = message.type.code,
                    waiter = waiter
                )

                try {
                    sendFrame(frame.copyOf())
                    val status = withTimeout(COMMAND_ACK_TIMEOUT_MS) {
                        waiter.await()
                    }
                    when (status) {
                        StatusCode.OK.code -> return@withLock
                        StatusCode.STALE_SESSION.code -> throw HarpStaleSessionException(
                            "Instrument rejected ${message.type} from session ${message.sid}"
                        )
                        else -> throw HarpCommandException(
                            "Instrument rejected ${message.type} with status=$status"
                        )
                    }
                } catch (exception: TimeoutCancellationException) {
                    if (attempt == MAX_COMMAND_ATTEMPTS - 1) {
                        throw HarpCommandTimeoutException(
                            "No ACK received for ${message.type}, seq=$sequence"
                        )
                    }
                } finally {
                    if (pendingAck?.waiter === waiter) {
                        pendingAck = null
                    }
                }
            }
        }
    }

    fun handleIncoming(frame: ByteArray): HarpSessionEvent {
        return when (val result = HarpCodec.decode(frame)) {
            is DecodeResult.Failure -> {
                if (state == State.HANDSHAKING && result.error == StatusCode.UNSUPPORTED_VERSION) {
                    deviceInfoWaiter?.completeExceptionally(
                        HarpHandshakeException("Instrument uses an unsupported protocol version")
                    )
                }
                HarpSessionEvent.InvalidFrame(result.error)
            }
            is DecodeResult.Success -> handleMessage(result.message)
        }
    }

    fun reset() {
        deviceInfoWaiter?.cancel(CancellationException("HARFA session reset"))
        deviceInfoWaiter = null
        pendingAck?.waiter?.cancel(CancellationException("HARFA session reset"))
        pendingAck = null
        endSession()
    }

    private fun endSession() {
        nextOutgoingSequence = 0
        lastIncomingSequence = null
        currentSid = SessionIds.NONE
        state = State.IDLE
    }

    private fun allocateSid(): Int {
        val sid = nextSid
        nextSid = if (sid == SessionIds.MAX) SessionIds.MIN else sid + 1
        return sid
    }

    private fun handleMessage(message: HarpMessage): HarpSessionEvent {
        return when (state) {
            State.IDLE -> HarpSessionEvent.StaleFrame(message, currentSid)
            State.HANDSHAKING -> handleHandshakeMessage(message)
            State.READY -> handleReadyMessage(message)
        }
    }

    private fun handleHandshakeMessage(message: HarpMessage): HarpSessionEvent {
        return when (message) {
            is HarpMessage.DeviceInfo -> {
                if (message.sid != currentSid) {
                    return HarpSessionEvent.StaleFrame(message, currentSid)
                }
                val waiter = deviceInfoWaiter ?: return HarpSessionEvent.StaleFrame(message, currentSid)
                lastIncomingSequence = message.seq
                state = State.READY
                nextOutgoingSequence = 1
                waiter.complete(message)
                HarpSessionEvent.Message(message)
            }

            is HarpMessage.Error -> {
                if (message.sid != currentSid && message.sid != SessionIds.NONE) {
                    return HarpSessionEvent.StaleFrame(message, currentSid)
                }
                deviceInfoWaiter?.completeExceptionally(
                    HarpHandshakeException(
                        "Instrument rejected HELLO with error ${message.code}"
                    )
                )
                HarpSessionEvent.Message(message)
            }

            else -> HarpSessionEvent.StaleFrame(message, currentSid)
        }
    }

    private fun handleReadyMessage(message: HarpMessage): HarpSessionEvent {
        if (message.sid != currentSid) {
            return HarpSessionEvent.StaleFrame(message, currentSid)
        }

        val sequenceGap = detectIncomingSequenceGap(message.seq)

        when (message) {
            is HarpMessage.Ack -> {
                val pending = pendingAck
                if (pending?.sequence == message.ackSeq) {
                    pending.waiter.complete(message.status)
                }
            }

            is HarpMessage.Error -> {
                if (
                    pendingAck?.sequence == message.refSeq &&
                    pendingAck?.typeCode == message.refType
                ) {
                    completePendingCommandException(
                        "Instrument reported error ${message.code} for seq=${message.refSeq}"
                    )
                }
            }

            else -> Unit
        }

        return HarpSessionEvent.Message(message, sequenceGap)
    }

    private fun detectIncomingSequenceGap(sequence: Int): SequenceGap? {
        val previousSequence = lastIncomingSequence
        lastIncomingSequence = sequence

        if (previousSequence == null) return null

        val expectedSequence = (previousSequence + 1) and 0xFF
        return if (sequence == expectedSequence) {
            null
        } else {
            SequenceGap(
                expectedSequence = expectedSequence,
                actualSequence = sequence
            )
        }
    }

    private fun completePendingCommandException(message: String) {
        val pending = pendingAck ?: return
        pending.waiter.completeExceptionally(HarpCommandException(message))
    }

    private data class PendingAck(
        val sequence: Int,
        val typeCode: Int,
        val waiter: CompletableDeferred<Int>
    )

    private enum class State {
        IDLE,
        HANDSHAKING,
        READY
    }

    private companion object {
        const val HELLO_SEQUENCE = 0
        const val MAX_HELLO_ATTEMPTS = 3
        const val HANDSHAKE_TIMEOUT_MS = 1_000L
        const val MAX_COMMAND_ATTEMPTS = 4
        const val COMMAND_ACK_TIMEOUT_MS = 500L
    }
}

internal class HarpHandshakeException(
    message: String
) : IOException(message)

internal open class HarpCommandException(
    message: String
) : IOException(message)

internal class HarpCommandTimeoutException(
    message: String
) : HarpCommandException(message)

internal class HarpStaleSessionException(
    message: String
) : HarpCommandException(message)
