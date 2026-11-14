package com.example.harpapp.domain.model

sealed interface ConnectionState {
    data object BLUETOOTH_DISABLED : ConnectionState
    data object PERMISSION_REQUIRED : ConnectionState
    data object SCANNING : ConnectionState
    data object CONNECTING : ConnectionState
    data object DISCOVERING_SERVICES : ConnectionState
    data object HANDSHAKING : ConnectionState
    data object CONNECTED : ConnectionState
    data object DISCONNECTING : ConnectionState
    data object DISCONNECTED : ConnectionState
    data class ERROR(val error: ConnectionError) : ConnectionState
}

enum class ConnectionErrorCode {
    SCAN_FAILED,
    CONNECTION_FAILED,
    SERVICE_DISCOVERY_FAILED,
    HANDSHAKE_FAILED,
    TIMEOUT,
    UNKNOWN
}

data class ConnectionError(
    val code: ConnectionErrorCode,
    val detail: String? = null
)

val ConnectionState.isConnected: Boolean
    get() = this == ConnectionState.CONNECTED

val ConnectionState.isOperationInProgress: Boolean
    get() = when (this) {
        ConnectionState.SCANNING,
        ConnectionState.CONNECTING,
        ConnectionState.DISCOVERING_SERVICES,
        ConnectionState.HANDSHAKING,
        ConnectionState.DISCONNECTING -> true
        else -> false
    }

class ConnectionStateMachine(
    initialState: ConnectionState = ConnectionState.DISCONNECTED
) {

    @Volatile
    var state: ConnectionState = initialState
        private set

    @Synchronized
    fun transitionTo(next: ConnectionState) {
        check(state == next || canTransition(state, next)) {
            "Invalid connection state transition: $state -> $next"
        }
        state = next
    }

    @Synchronized
    fun startScanning() {
        check(state.canStartOperation) {
            "Cannot start scanning from connection state: $state"
        }
        transitionTo(ConnectionState.SCANNING)
    }

    @Synchronized
    fun startConnecting() {
        check(state.canStartOperation) {
            "Cannot start connecting from connection state: $state"
        }
        transitionTo(ConnectionState.CONNECTING)
    }

    @Synchronized
    fun startDisconnecting() {
        check(state.isOperationInProgress || state == ConnectionState.CONNECTED) {
            "Cannot start disconnecting from connection state: $state"
        }
        transitionTo(ConnectionState.DISCONNECTING)
    }

    @Synchronized
    fun fail(error: ConnectionError) {
        transitionTo(ConnectionState.ERROR(error))
    }

    @Synchronized
    fun completeDisconnect() {
        transitionTo(ConnectionState.DISCONNECTED)
    }

    private val ConnectionState.canStartOperation: Boolean
        get() = this == ConnectionState.DISCONNECTED || this is ConnectionState.ERROR

    private fun canTransition(
        from: ConnectionState,
        to: ConnectionState
    ): Boolean {
        if (to is ConnectionState.ERROR) return true

        return when (from) {
            ConnectionState.BLUETOOTH_DISABLED ->
                to == ConnectionState.DISCONNECTED ||
                    to == ConnectionState.PERMISSION_REQUIRED ||
                    to == ConnectionState.SCANNING ||
                    to == ConnectionState.CONNECTING

            ConnectionState.PERMISSION_REQUIRED ->
                to == ConnectionState.DISCONNECTED ||
                    to == ConnectionState.BLUETOOTH_DISABLED ||
                    to == ConnectionState.SCANNING ||
                    to == ConnectionState.CONNECTING

            ConnectionState.SCANNING ->
                to == ConnectionState.DISCONNECTED ||
                    to == ConnectionState.CONNECTING ||
                    to == ConnectionState.DISCONNECTING ||
                    to == ConnectionState.BLUETOOTH_DISABLED ||
                    to == ConnectionState.PERMISSION_REQUIRED

            ConnectionState.CONNECTING ->
                to == ConnectionState.DISCOVERING_SERVICES ||
                    to == ConnectionState.DISCONNECTING ||
                    to == ConnectionState.DISCONNECTED ||
                    to == ConnectionState.BLUETOOTH_DISABLED ||
                    to == ConnectionState.PERMISSION_REQUIRED

            ConnectionState.DISCOVERING_SERVICES ->
                to == ConnectionState.HANDSHAKING ||
                    to == ConnectionState.DISCONNECTING ||
                    to == ConnectionState.DISCONNECTED ||
                    to == ConnectionState.BLUETOOTH_DISABLED ||
                    to == ConnectionState.PERMISSION_REQUIRED

            ConnectionState.HANDSHAKING ->
                to == ConnectionState.CONNECTED ||
                    to == ConnectionState.DISCONNECTING ||
                    to == ConnectionState.DISCONNECTED ||
                    to == ConnectionState.BLUETOOTH_DISABLED ||
                    to == ConnectionState.PERMISSION_REQUIRED

            ConnectionState.CONNECTED ->
                to == ConnectionState.DISCONNECTING ||
                    to == ConnectionState.DISCONNECTED ||
                    to == ConnectionState.BLUETOOTH_DISABLED ||
                    to == ConnectionState.PERMISSION_REQUIRED

            ConnectionState.DISCONNECTING ->
                to == ConnectionState.DISCONNECTED ||
                    to == ConnectionState.BLUETOOTH_DISABLED ||
                    to == ConnectionState.PERMISSION_REQUIRED

            ConnectionState.DISCONNECTED ->
                to == ConnectionState.BLUETOOTH_DISABLED ||
                    to == ConnectionState.PERMISSION_REQUIRED ||
                    to == ConnectionState.SCANNING ||
                    to == ConnectionState.CONNECTING

            is ConnectionState.ERROR ->
                to == ConnectionState.DISCONNECTED ||
                    to == ConnectionState.BLUETOOTH_DISABLED ||
                    to == ConnectionState.PERMISSION_REQUIRED ||
                    to == ConnectionState.SCANNING ||
                    to == ConnectionState.CONNECTING
        }
    }
}
