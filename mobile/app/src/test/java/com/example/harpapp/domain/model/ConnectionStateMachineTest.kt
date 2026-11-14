package com.example.harpapp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionStateMachineTest {

    @Test
    fun followsTheConnectionLifecycle() {
        val machine = ConnectionStateMachine()

        machine.startScanning()
        machine.transitionTo(ConnectionState.CONNECTING)
        machine.transitionTo(ConnectionState.DISCOVERING_SERVICES)
        machine.transitionTo(ConnectionState.HANDSHAKING)
        machine.transitionTo(ConnectionState.CONNECTED)

        assertEquals(ConnectionState.CONNECTED, machine.state)
        assertTrue(machine.state.isConnected)
        assertFalse(machine.state.isOperationInProgress)
    }

    @Test
    fun rejectsParallelScanAndConnectionAttempts() {
        val machine = ConnectionStateMachine()

        machine.startScanning()

        assertIllegalState { machine.startScanning() }
        assertIllegalState { machine.startConnecting() }
        assertTrue(machine.state.isOperationInProgress)
    }

    @Test
    fun reportsErrorAndAllowsAConnectionRetry() {
        val machine = ConnectionStateMachine()
        val error = ConnectionError(
            code = ConnectionErrorCode.HANDSHAKE_FAILED,
            detail = "DEVICE_INFO was not received"
        )

        machine.startConnecting()
        machine.fail(error)

        assertEquals(ConnectionState.ERROR(error), machine.state)
        assertFalse(machine.state.isConnected)

        machine.startConnecting()
        assertEquals(ConnectionState.CONNECTING, machine.state)
    }

    @Test
    fun cancellationFinishesInDisconnectedState() {
        val machine = ConnectionStateMachine()

        machine.startConnecting()
        machine.startDisconnecting()
        machine.completeDisconnect()

        assertEquals(ConnectionState.DISCONNECTED, machine.state)
        assertFalse(machine.state.isOperationInProgress)
    }

    @Test
    fun rejectsSkippingRequiredConnectionStages() {
        val machine = ConnectionStateMachine()

        assertIllegalState {
            machine.transitionTo(ConnectionState.CONNECTED)
        }
    }

    private fun assertIllegalState(action: () -> Unit) {
        try {
            action()
        } catch (_: IllegalStateException) {
            return
        }
        throw AssertionError("Expected IllegalStateException")
    }
}
