package com.example.harpapp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioPlaybackProgressTest {

    @Test
    fun emptyProgressHasNoFraction() {
        assertEquals(0f, AudioPlaybackProgress.EMPTY.fraction)
    }

    @Test
    fun fractionReflectsPlaybackPosition() {
        val progress = AudioPlaybackProgress(positionMs = 250L, durationMs = 1000L)

        assertEquals(0.25f, progress.fraction)
    }

    @Test
    fun fractionIsClampedToValidRange() {
        assertEquals(
            0f,
            AudioPlaybackProgress(positionMs = -100L, durationMs = 1000L).fraction
        )
        assertEquals(
            1f,
            AudioPlaybackProgress(positionMs = 1200L, durationMs = 1000L).fraction
        )
    }
}
