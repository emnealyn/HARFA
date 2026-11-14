package com.example.harpapp.presentation.viewmodel

import com.example.harpapp.device.simulator.FakeInstrumentRepository
import com.example.harpapp.domain.model.ConnectionError
import com.example.harpapp.domain.model.ConnectionErrorCode
import com.example.harpapp.domain.model.ConnectionState
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HarpViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun exposesDetailedConnectionStatesToTheHarpScreen() = runTest(dispatcher) {
        val repository = FakeInstrumentRepository()
        val viewModel = HarpViewModel(repository)
        val observer = backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
            viewModel.uiState.collect { }
        }

        repository.simulateConnectionState(ConnectionState.CONNECTING)
        assertEquals(
            HarpUiState.Connecting(ConnectionState.CONNECTING),
            viewModel.uiState.value
        )

        repository.simulateConnectionState(ConnectionState.DISCOVERING_SERVICES)
        assertEquals(
            HarpUiState.Connecting(ConnectionState.DISCOVERING_SERVICES),
            viewModel.uiState.value
        )

        repository.simulateConnectionState(ConnectionState.HANDSHAKING)
        assertEquals(
            HarpUiState.Connecting(ConnectionState.HANDSHAKING),
            viewModel.uiState.value
        )

        repository.simulateConnectionState(ConnectionState.CONNECTED)
        assertTrue(viewModel.uiState.value is HarpUiState.Connected)

        repository.simulateConnectionState(ConnectionState.BLUETOOTH_DISABLED)
        assertEquals(
            HarpUiState.Unavailable(ConnectionState.BLUETOOTH_DISABLED),
            viewModel.uiState.value
        )

        repository.simulateConnectionState(ConnectionState.PERMISSION_REQUIRED)
        assertEquals(
            HarpUiState.Unavailable(ConnectionState.PERMISSION_REQUIRED),
            viewModel.uiState.value
        )

        val error = ConnectionError(ConnectionErrorCode.TIMEOUT)
        repository.simulateConnectionState(ConnectionState.ERROR(error))
        assertEquals(HarpUiState.Error(error), viewModel.uiState.value)

        observer.cancel()
    }
}
