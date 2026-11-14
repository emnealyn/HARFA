package com.example.harpapp.initialization

sealed interface AppInitializationState {
    data object Loading : AppInitializationState
    data object Ready : AppInitializationState
    data class Error(val message: String) : AppInitializationState
}
