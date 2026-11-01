package com.example.harpapp.domain.repository

interface AudioPlayer {
    fun toggle(assetPath: String): Boolean

    fun release()
}
