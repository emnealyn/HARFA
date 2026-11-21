package com.example.harpapp.domain.model

data class AudioPlaybackProgress(
    val positionMs: Long = 0L,
    val durationMs: Long = 0L
) {
    val fraction: Float
        get() = if (durationMs <= 0L) {
            0f
        } else {
            (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        }

    companion object {
        val EMPTY = AudioPlaybackProgress()
    }
}
