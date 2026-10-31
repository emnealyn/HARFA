package com.example.harpapp.data

import android.content.Context

object MidiDurationHelper {

    fun getDurationMs(context: Context, midiFileName: String): Long {
        return try {
            val bytes = context.assets.open(midiFileName).readBytes()
            var i = 0

            val timeDivision = ((bytes[12].toInt() and 0xFF) shl 8) or (bytes[13].toInt() and 0xFF)
            var tempo = 500000L
            var lastTick = 0L

            i = 14
            while (i < bytes.size - 8) {
                if (bytes[i] == 'M'.code.toByte() && bytes[i+1] == 'T'.code.toByte() &&
                    bytes[i+2] == 'r'.code.toByte() && bytes[i+3] == 'k'.code.toByte()) {

                    val trackLen = ((bytes[i+4].toLong() and 0xFF) shl 24) or
                            ((bytes[i+5].toLong() and 0xFF) shl 16) or
                            ((bytes[i+6].toLong() and 0xFF) shl 8) or
                            (bytes[i+7].toLong() and 0xFF)

                    var tick = 0L
                    var j = i + 8
                    val trackEnd = j + trackLen.toInt()

                    while (j < trackEnd && j < bytes.size) {
                        var delta = 0L
                        var b: Int
                        do {
                            b = bytes[j++].toInt() and 0xFF
                            delta = (delta shl 7) or (b and 0x7F).toLong()
                        } while (b and 0x80 != 0)
                        tick += delta

                        if (tick > lastTick) lastTick = tick

                        val status = bytes[j].toInt() and 0xFF
                        when {
                            status == 0xFF -> {
                                val type = bytes[j+1].toInt() and 0xFF
                                j += 2
                                var metaLen = 0L
                                do {
                                    b = bytes[j++].toInt() and 0xFF
                                    metaLen = (metaLen shl 7) or (b and 0x7F).toLong()
                                } while (b and 0x80 != 0)
                                if (type == 0x51 && metaLen == 3L) {
                                    tempo = ((bytes[j].toLong() and 0xFF) shl 16) or
                                            ((bytes[j+1].toLong() and 0xFF) shl 8) or
                                            (bytes[j+2].toLong() and 0xFF)
                                }
                                j += metaLen.toInt()
                            }
                            status and 0xF0 == 0x90 || status and 0xF0 == 0x80 -> { j += 3 }
                            status and 0xF0 == 0xA0 || status and 0xF0 == 0xB0 -> { j += 3 }
                            status and 0xF0 == 0xC0 || status and 0xF0 == 0xD0 -> { j += 2 }
                            status and 0xF0 == 0xE0 -> { j += 3 }
                            status == 0xF0 -> {
                                j++
                                while (j < bytes.size && bytes[j].toInt() and 0xFF != 0xF7) j++
                                j++
                            }
                            else -> j++
                        }
                    }
                    i += 8 + trackLen.toInt()
                } else {
                    i++
                }
            }

            ((lastTick.toDouble() / timeDivision) * (tempo / 1000.0)).toLong()

        } catch (e: Exception) {
            0L
        }
    }

    fun formatDuration(durationMs: Long): String {
        val totalSeconds = (durationMs / 1000).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "$minutes:${"$seconds".padStart(2, '0')}"
    }
}
