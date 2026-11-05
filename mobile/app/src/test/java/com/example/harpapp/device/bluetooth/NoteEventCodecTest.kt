package com.example.harpapp.device.bluetooth

import com.example.harpapp.domain.model.NotePlayed
import org.junit.Assert.assertEquals
import org.junit.Test

class NoteEventCodecTest {

    private val codec = NoteEventCodec()

    @Test
    fun decodesSupportedNotesAndIgnoresProtocolCharacters() {
        val result = codec.decode("g\nxC4-H")

        assertEquals(
            listOf(
                NotePlayed("G"),
                NotePlayed("C"),
                NotePlayed("H")
            ),
            result
        )
    }

    @Test
    fun decodesOnlyTheRequestedByteRange() {
        val message = "C-G".toByteArray()

        val result = codec.decode(message, length = 1)

        assertEquals(listOf(NotePlayed("C")), result)
    }
}
