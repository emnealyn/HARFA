package com.example.harpapp.domain.repository

import com.example.harpapp.domain.model.AudioPlaybackState
import kotlinx.coroutines.flow.StateFlow

interface AudioPlayer {
    val playbackState: StateFlow<AudioPlaybackState>

    fun toggle(assetPath: String, onCompleted: () -> Unit): Boolean

    fun release()
}
