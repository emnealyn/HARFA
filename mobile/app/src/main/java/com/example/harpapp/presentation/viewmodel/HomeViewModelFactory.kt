package com.example.harpapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.harpapp.domain.usecase.FilterSongsUseCase
import com.example.harpapp.domain.usecase.ObserveSongsUseCase
import com.example.harpapp.domain.usecase.SetFavoriteUseCase

class HomeViewModelFactory(
    private val observeSongsUseCase: ObserveSongsUseCase,
    private val filterSongsUseCase: FilterSongsUseCase,
    private val setFavoriteUseCase: SetFavoriteUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(
                observeSongsUseCase = observeSongsUseCase,
                filterSongsUseCase = filterSongsUseCase,
                setFavoriteUseCase = setFavoriteUseCase
            ) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
