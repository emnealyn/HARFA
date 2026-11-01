package com.example.harpapp.domain.model

data class PairedDevice(
    val name: String?,
    val address: String
)

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

data class NotePlayed(
    val note: String
)
