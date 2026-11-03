package com.example.harpapp.protocol

// Wiadomości protokołu BLE. Pola u8/u16 są przechowywane jako Int, pole u32 jako Long.
sealed class HarpMessage(val type: MessageType) {
    abstract val seq: Int

    data class Hello(
        override val seq: Int,
        val protoMajor: Int,
        val protoMinor: Int
    ) : HarpMessage(MessageType.HELLO)

    data class DeviceInfo(
        override val seq: Int,
        val protoMajor: Int,
        val protoMinor: Int,
        val fwMajor: Int,
        val fwMinor: Int,
        val fwPatch: Int,
        val hwRev: Int,
        val stringCount: Int,
        val caps: Int,
        val deviceId: Long
    ) : HarpMessage(MessageType.DEVICE_INFO)

    data class StateRequest(
        override val seq: Int,
        val what: Int
    ) : HarpMessage(MessageType.STATE_REQUEST)

    // status jako Int, bo nowe kody mogą pojawić się w kolejnych wersjach minor
    data class Ack(
        override val seq: Int,
        val ackSeq: Int,
        val status: Int
    ) : HarpMessage(MessageType.ACK)

    data class Error(
        override val seq: Int,
        val code: Int,
        val refSeq: Int,
        val refType: Int
    ) : HarpMessage(MessageType.ERROR)

    data class BatteryState(
        override val seq: Int,
        val percent: Int,
        val millivolts: Int,
        val flags: Int
    ) : HarpMessage(MessageType.BATTERY_STATE)

    data class StringEvent(
        override val seq: Int,
        val stringId: Int,
        val midi: Int,
        val tMs: Int
    ) : HarpMessage(MessageType.STRING_EVENT)

    // Wartości logiczne -1, 0, +1 (bez offsetu +1 stosowanego w ramce)
    data class ConfigState(
        override val seq: Int,
        val octaveShift: Int,
        val lever: List<Int>,
        val baseOctave: Int
    ) : HarpMessage(MessageType.CONFIG_STATE)

    data class LedSet(
        override val seq: Int,
        val mask: Int,
        val mode: Int
    ) : HarpMessage(MessageType.LED_SET)

    data class LessonStart(
        override val seq: Int,
        val totalSteps: Int
    ) : HarpMessage(MessageType.LESSON_START)

    data class LessonStep(
        override val seq: Int,
        val index: Int,
        val mask: Int
    ) : HarpMessage(MessageType.LESSON_STEP)

    data class LessonResult(
        override val seq: Int,
        val result: Int
    ) : HarpMessage(MessageType.LESSON_RESULT)

    data class LessonEnd(
        override val seq: Int
    ) : HarpMessage(MessageType.LESSON_END)
}
