package com.example.harpapp.domain.repository

interface AudioPlayer {
    fun toggle(assetPath: String, onCompleted: () -> Unit): Boolean

    fun release()
}
