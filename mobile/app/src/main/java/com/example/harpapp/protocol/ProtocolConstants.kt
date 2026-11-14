package com.example.harpapp.protocol

import java.util.UUID

// Stałe protokołu BLE w wersji 2.0, zgodnie z docs/protocol/ble-protocol.md
object HarpProtocol {
    const val VERSION_MAJOR = 2
    const val VERSION_MINOR = 0

    const val HEADER_SIZE = 5
    const val MAX_PAYLOAD_SIZE = 15
    const val MAX_FRAME_SIZE = HEADER_SIZE + MAX_PAYLOAD_SIZE

    const val STRING_COUNT = 15
    const val LEVER_COUNT = 7

    val SERVICE_UUID: UUID = UUID.fromString("3CFB0001-6F66-44DE-A34A-0BCE651D326C")
    val RX_CHARACTERISTIC_UUID: UUID = UUID.fromString("3CFB0002-6F66-44DE-A34A-0BCE651D326C")
    val TX_CHARACTERISTIC_UUID: UUID = UUID.fromString("3CFB0003-6F66-44DE-A34A-0BCE651D326C")
}

enum class MessageType(val code: Int, val minPayloadSize: Int) {
    HELLO(0x01, 2),
    DEVICE_INFO(0x02, 13),
    STATE_REQUEST(0x03, 1),
    ACK(0x10, 2),
    ERROR(0x11, 3),
    BATTERY_STATE(0x12, 4),
    STRING_EVENT(0x20, 4),
    CONFIG_STATE(0x21, 9),
    LED_SET(0x30, 3),
    LESSON_START(0x31, 2),
    LESSON_STEP(0x32, 4),
    LESSON_RESULT(0x33, 1),
    LESSON_END(0x34, 0),
    LESSON_PAUSE(0x35, 1);

    companion object {
        fun fromCode(code: Int): MessageType? = entries.firstOrNull { it.code == code }
    }
}

enum class StatusCode(val code: Int) {
    OK(0x00),
    UNSUPPORTED_VERSION(0x01),
    UNKNOWN_TYPE(0x02),
    MALFORMED(0x03),
    INVALID_ARGUMENT(0x04),
    NOT_READY(0x05),
    BUSY(0x06),
    INTERNAL(0x07),
    STALE_SESSION(0x08);

    companion object {
        // Nowe kody mogą pojawić się w kolejnych wersjach minor, dlatego wynik może być null
        fun fromCode(code: Int): StatusCode? = entries.firstOrNull { it.code == code }
    }
}

object Capabilities {
    const val LEDS = 1 shl 0
    const val BATTERY = 1 shl 1
    const val LESSON = 1 shl 2
    const val DISPLAY = 1 shl 3
    const val KNOWN_MASK = LEDS or BATTERY or LESSON or DISPLAY
}

object StateRequestFlags {
    const val CONFIG = 1 shl 0
    const val BATTERY = 1 shl 1
    const val KNOWN_MASK = CONFIG or BATTERY
}

object BatteryFlags {
    const val CHARGING = 1 shl 0
    const val EXTERNAL_POWER = 1 shl 1
    const val LOW = 1 shl 2
    const val KNOWN_MASK = CHARGING or EXTERNAL_POWER or LOW
}

object LedMode {
    const val STEADY = 0x00
    const val BLINK = 0x01
}

object LessonResultCode {
    const val MISS = 0x00
    const val HIT = 0x01
    const val COMPLETE = 0x02
}

object LessonPauseCode {
    const val RESUME = 0x00
    const val PAUSE = 0x01
}

object SessionIds {
    const val NONE = 0x00
    const val MIN = 0x01
    const val MAX = 0xFF
}

object BatteryPercent {
    const val UNKNOWN = 0xFF
}
