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
