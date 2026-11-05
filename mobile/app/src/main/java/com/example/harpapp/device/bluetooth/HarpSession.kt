package com.example.harpapp.device.bluetooth

import com.example.harpapp.protocol.DecodeResult
import com.example.harpapp.protocol.HarpCodec
import com.example.harpapp.protocol.HarpMessage
import com.example.harpapp.protocol.HarpProtocol
import com.example.harpapp.protocol.StatusCode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.io.IOException

internal sealed interface HarpSessionEvent {
    data class Message(val message: HarpMessage) : HarpSessionEvent

    data class InvalidFrame(val status: StatusCode) : HarpSessionEvent
}

internal class HarpSession(
    private val sendFrame: suspend (ByteArray) -> Unit
) {

    private var state = State.IDLE
    private var deviceInfoWaiter: CompletableDeferred<HarpMessage.DeviceInfo>? = null
    private var pendingAck: PendingAck? = null
    private var nextOutgoingSequence = 0
    private val commandMutex = Mutex()

    suspend fun start(): HarpMessage.DeviceInfo {
        reset()
        state = State.HANDSHAKING

        repeat(MAX_HELLO_ATTEMPTS) { attempt ->
            val waiter = CompletableDeferred<HarpMessage.DeviceInfo>()
            deviceInfoWaiter = waiter

            try {
                sendFrame(
                    HarpCodec.encode(
                        HarpMessage.Hello(
                            seq = HELLO_SEQUENCE,
                            protoMajor = HarpProtocol.VERSION_MAJOR,
                            protoMinor = HarpProtocol.VERSION_MINOR
                        )
                    )
                )

                val deviceInfo = withTimeout(HANDSHAKE_TIMEOUT_MS) {
                    waiter.await()
                }
                state = State.READY
                nextOutgoingSequence = 1
                deviceInfoWaiter = null
                return deviceInfo
            } catch (exception: TimeoutCancellationException) {
                deviceInfoWaiter = null
                if (attempt == MAX_HELLO_ATTEMPTS - 1) {
                    state = State.IDLE
                    throw HarpHandshakeException(
                        "No DEVICE_INFO received after $MAX_HELLO_ATTEMPTS HELLO attempts"
                    )
                }
            } catch (exception: HarpHandshakeException) {
                deviceInfoWaiter = null
                state = State.IDLE
                throw exception
            } catch (exception: CancellationException) {
                deviceInfoWaiter = null
                state = State.IDLE
                throw exception
            } catch (exception: IOException) {
                deviceInfoWaiter = null
                state = State.IDLE
                throw exception
            }
        }

        error("Handshake did not complete")
    }

    suspend fun sendCommand(messageFactory: (Int) -> HarpMessage) {
        commandMutex.withLock {
            check(state == State.READY) { "HARFA session is not ready" }

            val sequence = nextOutgoingSequence
            nextOutgoingSequence = (nextOutgoingSequence + 1) and 0xFF
            val message = messageFactory(sequence)
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
                    if (status != StatusCode.OK.code) {
                        throw HarpCommandException(
                            "Instrument rejected ${message.type} with status=$status"
                        )
                    }
                    return@withLock
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
            is DecodeResult.Failure -> HarpSessionEvent.InvalidFrame(result.error)
            is DecodeResult.Success -> handleMessage(result.message)
        }
    }

    fun reset() {
        deviceInfoWaiter?.cancel(CancellationException("HARFA session reset"))
        deviceInfoWaiter = null
        pendingAck?.waiter?.cancel(CancellationException("HARFA session reset"))
        pendingAck = null
        nextOutgoingSequence = 0
        state = State.IDLE
    }

    private fun handleMessage(message: HarpMessage): HarpSessionEvent {
        if (state == State.HANDSHAKING) {
            when (message) {
                is HarpMessage.DeviceInfo -> {
                    deviceInfoWaiter?.complete(message)
                    return HarpSessionEvent.Message(message)
                }

                is HarpMessage.Error -> {
                    deviceInfoWaiter?.completeExceptionally(
                        HarpHandshakeException(
                            "Instrument rejected HELLO with error ${message.code}"
                        )
                    )
                    return HarpSessionEvent.Message(message)
                }

                else -> Unit
            }
        }

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

        return HarpSessionEvent.Message(message)
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
