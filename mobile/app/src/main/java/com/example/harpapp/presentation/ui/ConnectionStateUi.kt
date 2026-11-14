package com.example.harpapp.presentation.ui

import com.example.harpapp.R
import com.example.harpapp.domain.model.ConnectionErrorCode
import com.example.harpapp.domain.model.ConnectionState

internal fun connectionStateTitle(state: ConnectionState): Int = when (state) {
    ConnectionState.BLUETOOTH_DISABLED -> R.string.connection_bluetooth_disabled
    ConnectionState.PERMISSION_REQUIRED -> R.string.connection_permission_required
    ConnectionState.SCANNING -> R.string.connection_scanning
    ConnectionState.CONNECTING -> R.string.connection_connecting
    ConnectionState.DISCOVERING_SERVICES -> R.string.connection_discovering_services
    ConnectionState.HANDSHAKING -> R.string.connection_handshaking
    ConnectionState.CONNECTED -> R.string.connected
    ConnectionState.DISCONNECTING -> R.string.connection_disconnecting
    ConnectionState.DISCONNECTED -> R.string.disconnected
    is ConnectionState.ERROR -> connectionErrorMessage(state.error.code)
}

internal fun connectionStateMessage(state: ConnectionState): Int = when (state) {
    ConnectionState.BLUETOOTH_DISABLED ->
        R.string.connection_bluetooth_disabled_message
    ConnectionState.PERMISSION_REQUIRED ->
        R.string.bluetooth_permissions_are_required
    ConnectionState.SCANNING ->
        R.string.connection_scanning_message
    ConnectionState.CONNECTING ->
        R.string.connection_connecting_message
    ConnectionState.DISCOVERING_SERVICES ->
        R.string.connection_discovering_services_message
    ConnectionState.HANDSHAKING ->
        R.string.connection_handshaking_message
    ConnectionState.CONNECTED,
    ConnectionState.DISCONNECTED ->
        R.string.no_device
    ConnectionState.DISCONNECTING ->
        R.string.connection_disconnecting_message
    is ConnectionState.ERROR ->
        R.string.connection_error_message
}

internal fun connectionErrorMessage(code: ConnectionErrorCode): Int = when (code) {
    ConnectionErrorCode.SCAN_FAILED -> R.string.bluetooth_scan_failed
    ConnectionErrorCode.CONNECTION_FAILED -> R.string.bluetooth_connection_failed
    ConnectionErrorCode.SERVICE_DISCOVERY_FAILED ->
        R.string.bluetooth_service_discovery_failed
    ConnectionErrorCode.HANDSHAKE_FAILED -> R.string.bluetooth_handshake_failed
    ConnectionErrorCode.TIMEOUT -> R.string.bluetooth_connection_timeout
    ConnectionErrorCode.UNKNOWN -> R.string.connection_error_message
}
