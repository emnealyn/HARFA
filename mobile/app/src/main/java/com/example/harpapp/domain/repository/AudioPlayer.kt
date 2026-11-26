package com.example.harpapp.domain.repository

import com.example.harpapp.domain.model.AudioPlaybackProgress
import com.example.harpapp.domain.model.AudioPlaybackState
import kotlinx.coroutines.flow.StateFlow

interface AudioPlayer {
    val playbackState: StateFlow<AudioPlaybackState>
    val playbackProgress: StateFlow<AudioPlaybackProgress>

    fun toggle(assetPath: String, onCompleted: () -> Unit): Boolean

    fun seekTo(positionMs: Long): Boolean

    fun release()
}
