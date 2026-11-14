package com.example.harpapp

import android.app.Application
import android.util.Log
import com.example.harpapp.di.AppContainer
import com.example.harpapp.initialization.AppInitializationState
import com.example.harpapp.initialization.AppInitializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HarpApplication : Application() {
    private val initializationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _initializationState =
        MutableStateFlow<AppInitializationState>(AppInitializationState.Loading)
    private var initializationJob: Job? = null

    val initializationState: StateFlow<AppInitializationState> =
        _initializationState.asStateFlow()

    val appContainer: AppContainer by lazy {
        AppContainer(this)
    }

    override fun onCreate() {
        super.onCreate()
        appContainer
        retryInitialization()
    }

    fun retryInitialization() {
        if (initializationJob?.isActive == true) return

        _initializationState.value = AppInitializationState.Loading
        initializationJob = initializationScope.launch {
            val state = try {
                AppInitializer(appContainer.catalogDataSource).initialize()
            } catch (exception: Exception) {
                Log.e(TAG, "Application initialization failed", exception)
                AppInitializationState.Error("Unexpected application initialization error")
            }
            _initializationState.value = state
        }
    }

    private companion object {
        const val TAG = "HarpApplication"
    }
}
