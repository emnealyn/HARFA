package com.example.harpapp.domain.model

sealed interface AudioPlaybackState {
    data object Idle : AudioPlaybackState

    data class Preparing(
        val assetPath: String
    ) : AudioPlaybackState

    data class Playing(
        val assetPath: String
    ) : AudioPlaybackState

    data class Paused(
        val assetPath: String
    ) : AudioPlaybackState

    data class Error(
        val assetPath: String?,
        val cause: Throwable
    ) : AudioPlaybackState
}
