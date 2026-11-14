package com.example.harpapp.protocol

import com.example.harpapp.protocol.HarpProtocol.HEADER_SIZE
import com.example.harpapp.protocol.HarpProtocol.LEVER_COUNT
import com.example.harpapp.protocol.HarpProtocol.MAX_PAYLOAD_SIZE
import com.example.harpapp.protocol.HarpProtocol.STRING_COUNT
import com.example.harpapp.protocol.HarpProtocol.VERSION_MAJOR

sealed interface DecodeResult {
    data class Success(val message: HarpMessage) : DecodeResult
    data class Failure(val error: StatusCode) : DecodeResult
}

// Kodek ramek protokołu BLE (docs/protocol/ble-protocol.md). Nie zależy od Androida.
object HarpCodec {

    private const val STRING_MASK_LIMIT = 1 shl STRING_COUNT

    fun encode(message: HarpMessage): ByteArray {
        require(hasValidArguments(message)) { "Invalid argument in $message" }

        val payload = PayloadWriter()
        when (message) {
            is HarpMessage.Hello -> payload.u8(message.protoMajor).u8(message.protoMinor)
            is HarpMessage.DeviceInfo -> payload
                .u8(message.protoMajor).u8(message.protoMinor)
                .u8(message.fwMajor).u8(message.fwMinor).u8(message.fwPatch)
                .u8(message.hwRev).u8(message.stringCount)
                .u16(message.caps).u32(message.deviceId)
            is HarpMessage.StateRequest -> payload.u8(message.what)
            is HarpMessage.Ack -> payload.u8(message.ackSeq).u8(message.status)
            is HarpMessage.Error -> payload.u8(message.code).u8(message.refSeq).u8(message.refType)
            is HarpMessage.BatteryState -> payload.u8(message.percent).u16(message.millivolts).u8(message.flags)
            is HarpMessage.StringEvent -> payload.u8(message.stringId).u8(message.midi).u16(message.tMs)
            is HarpMessage.ConfigState -> {
                payload.u8(message.octaveShift + 1)
                message.lever.forEach { payload.u8(it + 1) }
                payload.u8(message.baseOctave)
            }
            is HarpMessage.LedSet -> payload.u16(message.mask).u8(message.mode)
            is HarpMessage.LessonStart -> payload.u16(message.totalSteps)
            is HarpMessage.LessonStep -> payload.u16(message.index).u16(message.mask)
            is HarpMessage.LessonResult -> payload.u8(message.result)
            is HarpMessage.LessonEnd -> Unit
            is HarpMessage.LessonPause -> payload.u8(message.paused)
        }

        val body = payload.toByteArray()
        val header = PayloadWriter()
            .u8(VERSION_MAJOR)
            .u8(message.type.code)
            .u8(message.seq)
            .u8(message.sid)
            .u8(body.size)
            .toByteArray()
        return header + body
    }

    // Kolejność sprawdzeń zgodna z sekcją 10.1 specyfikacji
    fun decode(frame: ByteArray): DecodeResult {
        if (frame.size < HEADER_SIZE) return DecodeResult.Failure(StatusCode.MALFORMED)

        val reader = PayloadReader(frame)
        val ver = reader.u8()
        val typeCode = reader.u8()
        val seq = reader.u8()
        val sid = reader.u8()
        val len = reader.u8()

        if (ver != VERSION_MAJOR) return DecodeResult.Failure(StatusCode.UNSUPPORTED_VERSION)
        if (len > MAX_PAYLOAD_SIZE || frame.size != HEADER_SIZE + len) {
            return DecodeResult.Failure(StatusCode.MALFORMED)
        }
        val type = MessageType.fromCode(typeCode)
            ?: return DecodeResult.Failure(StatusCode.UNKNOWN_TYPE)
        if (len < type.minPayloadSize) return DecodeResult.Failure(StatusCode.MALFORMED)

        // Nadmiarowe bajty na końcu payloadu są pomijane (zgodność z przyszłymi wersjami minor)
        val message = when (type) {
            MessageType.HELLO -> HarpMessage.Hello(seq, sid, reader.u8(), reader.u8())
            MessageType.DEVICE_INFO -> HarpMessage.DeviceInfo(
                seq = seq,
                sid = sid,
                protoMajor = reader.u8(),
                protoMinor = reader.u8(),
                fwMajor = reader.u8(),
                fwMinor = reader.u8(),
                fwPatch = reader.u8(),
                hwRev = reader.u8(),
                stringCount = reader.u8(),
                caps = reader.u16() and Capabilities.KNOWN_MASK,
                deviceId = reader.u32()
            )
            MessageType.STATE_REQUEST -> HarpMessage.StateRequest(seq, sid, reader.u8() and StateRequestFlags.KNOWN_MASK)
            MessageType.ACK -> HarpMessage.Ack(seq, sid, reader.u8(), reader.u8())
            MessageType.ERROR -> HarpMessage.Error(seq, sid, reader.u8(), reader.u8(), reader.u8())
            MessageType.BATTERY_STATE -> HarpMessage.BatteryState(
                seq = seq,
                sid = sid,
                percent = reader.u8(),
                millivolts = reader.u16(),
                flags = reader.u8() and BatteryFlags.KNOWN_MASK
            )
            MessageType.STRING_EVENT -> HarpMessage.StringEvent(seq, sid, reader.u8(), reader.u8(), reader.u16())
            MessageType.CONFIG_STATE -> HarpMessage.ConfigState(
                seq = seq,
                sid = sid,
                octaveShift = reader.u8() - 1,
                lever = List(LEVER_COUNT) { reader.u8() - 1 },
                baseOctave = reader.u8()
            )
            MessageType.LED_SET -> HarpMessage.LedSet(seq, sid, reader.u16(), reader.u8())
            MessageType.LESSON_START -> HarpMessage.LessonStart(seq, sid, reader.u16())
            MessageType.LESSON_STEP -> HarpMessage.LessonStep(seq, sid, reader.u16(), reader.u16())
            MessageType.LESSON_RESULT -> HarpMessage.LessonResult(seq, sid, reader.u8())
            MessageType.LESSON_END -> HarpMessage.LessonEnd(seq, sid)
            MessageType.LESSON_PAUSE -> HarpMessage.LessonPause(seq, sid, reader.u8())
        }

        return if (hasValidArguments(message)) {
            DecodeResult.Success(message)
        } else {
            DecodeResult.Failure(StatusCode.INVALID_ARGUMENT)
        }
    }

    private fun hasValidArguments(message: HarpMessage): Boolean = when (message) {
        is HarpMessage.Hello -> message.sid != SessionIds.NONE
        is HarpMessage.StringEvent -> message.stringId < STRING_COUNT
        is HarpMessage.ConfigState ->
            message.octaveShift in -1..1 &&
                message.lever.size == LEVER_COUNT &&
                message.lever.all { it in -1..1 }
        is HarpMessage.BatteryState -> message.percent <= 100 || message.percent == BatteryPercent.UNKNOWN
        is HarpMessage.LedSet ->
            message.mask in 0 until STRING_MASK_LIMIT &&
                (message.mode == LedMode.STEADY || message.mode == LedMode.BLINK)
        is HarpMessage.LessonStep -> message.mask in 0 until STRING_MASK_LIMIT
        is HarpMessage.LessonResult -> message.result in LessonResultCode.MISS..LessonResultCode.COMPLETE
        is HarpMessage.LessonPause -> message.paused in LessonPauseCode.RESUME..LessonPauseCode.PAUSE
        else -> true
    }

    private class PayloadReader(private val bytes: ByteArray) {
        private var offset = 0

        fun u8(): Int = bytes[offset++].toInt() and 0xFF

        fun u16(): Int = u8() or (u8() shl 8)

        fun u32(): Long = u16().toLong() or (u16().toLong() shl 16)
    }

    private class PayloadWriter {
        private val bytes = ArrayList<Byte>(MAX_PAYLOAD_SIZE)

        fun u8(value: Int) = apply {
            require(value in 0..0xFF) { "Value $value does not fit in u8" }
            bytes.add(value.toByte())
        }

        fun u16(value: Int) = apply {
            require(value in 0..0xFFFF) { "Value $value does not fit in u16" }
            u8(value and 0xFF)
            u8(value ushr 8)
        }

        fun u32(value: Long) = apply {
            require(value in 0..0xFFFF_FFFFL) { "Value $value does not fit in u32" }
            u16((value and 0xFFFF).toInt())
            u16((value ushr 16).toInt())
        }

        fun toByteArray(): ByteArray = bytes.toByteArray()
    }
}
