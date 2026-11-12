package com.example.harpapp.protocol

import com.google.gson.JsonObject
import com.google.gson.JsonParser

// Wczytuje docs/protocol/test-vectors.json (dodany do zasobów testowych w build.gradle.kts)
object TestVectors {

    private const val RESOURCE = "test-vectors.json"

    val all: List<JsonObject> by lazy {
        val stream = requireNotNull(javaClass.classLoader?.getResourceAsStream(RESOURCE)) {
            "Missing $RESOURCE on the test classpath"
        }
        val root = stream.reader(Charsets.UTF_8).use { JsonParser.parseReader(it).asJsonObject }
        root.getAsJsonArray("vectors").map { it.asJsonObject }
    }

    fun bytes(vector: JsonObject): ByteArray {
        val hex = vector.get("hex").asString
        if (hex.isEmpty()) return ByteArray(0)
        return hex.split(" ").map { it.toInt(16).toByte() }.toByteArray()
    }

    fun expectedMessage(vector: JsonObject): HarpMessage {
        val d = vector.getAsJsonObject("decoded")
        fun int(name: String) = d.get(name).asInt
        val seq = int("seq")
        val sid = int("sid")

        return when (val type = d.get("type").asString) {
            "HELLO" -> HarpMessage.Hello(seq, sid, int("proto_major"), int("proto_minor"))
            "DEVICE_INFO" -> HarpMessage.DeviceInfo(
                seq = seq,
                sid = sid,
                protoMajor = int("proto_major"),
                protoMinor = int("proto_minor"),
                fwMajor = int("fw_major"),
                fwMinor = int("fw_minor"),
                fwPatch = int("fw_patch"),
                hwRev = int("hw_rev"),
                stringCount = int("string_count"),
                caps = int("caps"),
                deviceId = d.get("device_id").asLong
            )
            "STATE_REQUEST" -> HarpMessage.StateRequest(seq, sid, int("what"))
            "ACK" -> HarpMessage.Ack(seq, sid, int("ack_seq"), int("status"))
            "ERROR" -> HarpMessage.Error(seq, sid, int("code"), int("ref_seq"), int("ref_type"))
            "BATTERY_STATE" -> HarpMessage.BatteryState(seq, sid, int("percent"), int("millivolts"), int("flags"))
            "STRING_EVENT" -> HarpMessage.StringEvent(seq, sid, int("string_id"), int("midi"), int("t_ms"))
            "CONFIG_STATE" -> HarpMessage.ConfigState(
                seq = seq,
                sid = sid,
                octaveShift = int("octave_shift"),
                lever = d.getAsJsonArray("lever").map { it.asInt },
                baseOctave = int("base_octave")
            )
            "LED_SET" -> HarpMessage.LedSet(seq, sid, int("mask"), int("mode"))
            "LESSON_START" -> HarpMessage.LessonStart(seq, sid, int("total_steps"))
            "LESSON_STEP" -> HarpMessage.LessonStep(seq, sid, int("index"), int("mask"))
            "LESSON_RESULT" -> HarpMessage.LessonResult(seq, sid, int("result"))
            "LESSON_END" -> HarpMessage.LessonEnd(seq, sid)
            "LESSON_PAUSE" -> HarpMessage.LessonPause(seq, sid, int("paused"))
            else -> throw IllegalArgumentException("Unknown message type in test vector: $type")
        }
    }
}
