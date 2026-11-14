package com.example.harpapp.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HarpCodecTest {

    @Test
    fun everyMessageTypeHasValidVector() {
        val coveredTypes = TestVectors.all
            .filter { it.get("valid").asBoolean }
            .map { it.getAsJsonObject("decoded").get("type").asString }
            .toSet()

        assertEquals(MessageType.entries.map { it.name }.toSet(), coveredTypes)
    }

    @Test
    fun everyValidVectorFitsInDefaultMtu() {
        TestVectors.all
            .filter { it.get("valid").asBoolean && it.get("roundtrip").asBoolean }
            .forEach { assertTrue(it.get("name").asString, TestVectors.bytes(it).size <= HarpProtocol.MAX_FRAME_SIZE) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun encodeRejectsStringOutOfRange() {
        HarpCodec.encode(HarpMessage.StringEvent(seq = 0, sid = 1, stringId = 15, midi = 62, tMs = 0))
    }

    @Test(expected = IllegalArgumentException::class)
    fun encodeRejectsLedMaskOutsideStrings() {
        HarpCodec.encode(HarpMessage.LedSet(seq = 0, sid = 1, mask = 0x8000, mode = LedMode.STEADY))
    }

    @Test(expected = IllegalArgumentException::class)
    fun encodeRejectsSeqOutsideU8() {
        HarpCodec.encode(HarpMessage.LessonEnd(seq = 256, sid = 1))
    }

    @Test(expected = IllegalArgumentException::class)
    fun encodeRejectsSidOutsideU8() {
        HarpCodec.encode(HarpMessage.LessonEnd(seq = 0, sid = 256))
    }

    @Test(expected = IllegalArgumentException::class)
    fun encodeRejectsHelloWithoutSession() {
        HarpCodec.encode(HarpMessage.Hello(seq = 0, sid = SessionIds.NONE, protoMajor = 2, protoMinor = 0))
    }

    @Test(expected = IllegalArgumentException::class)
    fun encodeRejectsUnknownPauseValue() {
        HarpCodec.encode(HarpMessage.LessonPause(seq = 0, sid = 1, paused = 2))
    }
}
