package com.example.harpapp.di

import android.content.Context
import com.example.harpapp.data.local.AppDatabase
import com.example.harpapp.data.local.RoomSongRepository
import com.example.harpapp.data.media.AndroidAssetAudioPlayer
import com.example.harpapp.data.preferences.DataStoreSettingsRepository
import com.example.harpapp.data.preferences.dataStore
import com.example.harpapp.device.bluetooth.BluetoothInstrumentRepository
import com.example.harpapp.domain.repository.AudioPlayer
import com.example.harpapp.domain.repository.InstrumentRepository
import com.example.harpapp.domain.repository.SettingsRepository
import com.example.harpapp.domain.repository.SongRepository
import com.example.harpapp.domain.usecase.FilterSongsUseCase
import com.example.harpapp.domain.usecase.GetSongUseCase
import com.example.harpapp.domain.usecase.ObserveSongsUseCase
import com.example.harpapp.domain.usecase.PrepareLessonUseCase
import com.example.harpapp.domain.usecase.SetFavoriteUseCase
import com.example.harpapp.presentation.viewmodel.BluetoothViewModelFactory
import com.example.harpapp.presentation.viewmodel.HomeViewModelFactory
import com.example.harpapp.presentation.viewmodel.SettingsViewModelFactory
import com.example.harpapp.presentation.viewmodel.SongViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(
    context: Context,
    instrumentRepositoryOverride: InstrumentRepository? = null
) {
    private val appContext = context.applicationContext
    private val databaseScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val database = AppDatabase.getDatabase(appContext, databaseScope)

    private val songRepository: SongRepository = RoomSongRepository(database.songDao())
    val settingsRepository: SettingsRepository =
        DataStoreSettingsRepository(appContext.dataStore)
    val audioPlayer: AudioPlayer = AndroidAssetAudioPlayer(appContext)
    val instrumentRepository: InstrumentRepository =
        instrumentRepositoryOverride ?: BluetoothInstrumentRepository(appContext)

    private val observeSongsUseCase = ObserveSongsUseCase(songRepository)
    private val getSongUseCase = GetSongUseCase(songRepository)
    private val setFavoriteUseCase = SetFavoriteUseCase(songRepository)
    private val filterSongsUseCase = FilterSongsUseCase()
    private val prepareLessonUseCase = PrepareLessonUseCase()

    val homeViewModelFactory = HomeViewModelFactory(
        observeSongsUseCase = observeSongsUseCase,
        filterSongsUseCase = filterSongsUseCase,
        setFavoriteUseCase = setFavoriteUseCase
    )

    val songViewModelFactory = SongViewModelFactory(
        getSongUseCase = getSongUseCase,
        prepareLessonUseCase = prepareLessonUseCase,
        audioPlayer = audioPlayer,
        noteEvents = instrumentRepository.noteEvents,
        connectionState = instrumentRepository.connectionState
    )

    val settingsViewModelFactory = SettingsViewModelFactory(settingsRepository)
    val bluetoothViewModelFactory = BluetoothViewModelFactory(instrumentRepository)
}
