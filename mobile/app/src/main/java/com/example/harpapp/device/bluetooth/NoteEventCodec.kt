package com.example.harpapp.device.bluetooth

import com.example.harpapp.domain.model.NotePlayed
import java.nio.charset.StandardCharsets
import java.util.Locale

class NoteEventCodec(
    private val supportedNotes: Set<Char> = setOf('A', 'B', 'C', 'D', 'E', 'F', 'G', 'H')
) {
    fun decode(bytes: ByteArray, length: Int): List<NotePlayed> {
        require(length in 0..bytes.size) {
            "Message length must be between 0 and ${bytes.size}, but was $length"
        }

        return decode(String(bytes, 0, length, StandardCharsets.UTF_8))
    }

    fun decode(message: String): List<NotePlayed> {
        return message
            .uppercase(Locale.ROOT)
            .mapNotNull { character ->
                character
                    .takeIf { it in supportedNotes }
                    ?.let { NotePlayed(it.toString()) }
            }
    }
}
