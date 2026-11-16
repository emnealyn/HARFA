package com.example.harpapp.di

import android.content.Context
import com.example.harpapp.data.catalog.AssetSongCatalogDataSource
import com.example.harpapp.data.catalog.SongCatalogDataSource
import com.example.harpapp.data.catalog.AssetTuningCatalogDataSource
import com.example.harpapp.data.catalog.TuningCatalogDataSource
import com.example.harpapp.data.local.AppDatabase
import com.example.harpapp.data.media.AndroidAssetAudioPlayer
import com.example.harpapp.data.preferences.DataStoreSettingsRepository
import com.example.harpapp.data.preferences.dataStore
import com.example.harpapp.data.repository.SongRepositoryImpl
import com.example.harpapp.data.repository.TuningRepositoryImpl
import com.example.harpapp.domain.repository.AudioPlayer
import com.example.harpapp.domain.repository.InstrumentRepository
import com.example.harpapp.domain.repository.SettingsRepository
import com.example.harpapp.domain.repository.SongRepository
import com.example.harpapp.domain.repository.TuningRepository
import com.example.harpapp.domain.usecase.AnalyzeTuningUseCase
import com.example.harpapp.domain.usecase.FilterSongsUseCase
import com.example.harpapp.domain.usecase.GetSongProgressUseCase
import com.example.harpapp.domain.usecase.GetSongTuningUseCase
import com.example.harpapp.domain.usecase.GetTuningsUseCase
import com.example.harpapp.domain.usecase.GetSongUseCase
import com.example.harpapp.domain.usecase.InstrumentLessonController
import com.example.harpapp.domain.usecase.ObserveSongsUseCase
import com.example.harpapp.domain.usecase.PrepareLessonUseCase
import com.example.harpapp.domain.usecase.SaveSongProgressUseCase
import com.example.harpapp.domain.usecase.SetFavoriteUseCase
import com.example.harpapp.presentation.dev.DevTools
import com.example.harpapp.presentation.viewmodel.BluetoothViewModelFactory
import com.example.harpapp.presentation.viewmodel.HarpViewModelFactory
import com.example.harpapp.presentation.viewmodel.HomeViewModelFactory
import com.example.harpapp.presentation.viewmodel.SettingsViewModelFactory
import com.example.harpapp.presentation.viewmodel.SongViewModelFactory

class AppContainer(
    context: Context,
    instrumentRepositoryOverride: InstrumentRepository? = null
) {
    private val appContext = context.applicationContext
    private val database = AppDatabase.getDatabase(appContext)

    val catalogDataSource: SongCatalogDataSource = AssetSongCatalogDataSource(appContext)
    private val songRepository: SongRepository = SongRepositoryImpl(
        catalogDataSource = catalogDataSource,
        favoriteDao = database.favoriteDao(),
        progressDao = database.progressDao()
    )
    val tuningCatalogDataSource: TuningCatalogDataSource = AssetTuningCatalogDataSource(appContext)
    private val tuningRepository: TuningRepository = TuningRepositoryImpl(tuningCatalogDataSource)

    val settingsRepository: SettingsRepository =
        DataStoreSettingsRepository(appContext.dataStore)
    val audioPlayer: AudioPlayer = AndroidAssetAudioPlayer(appContext)
    private val instrumentSetup: InstrumentSetup? =
        if (instrumentRepositoryOverride == null)
            InstrumentModule.create(appContext) else null
    val instrumentRepository: InstrumentRepository =
        instrumentRepositoryOverride ?: instrumentSetup!!.repository

    val devTools: DevTools? = instrumentSetup?.devTools

    private val observeSongsUseCase = ObserveSongsUseCase(songRepository)
    private val getSongUseCase = GetSongUseCase(songRepository)
    private val setFavoriteUseCase = SetFavoriteUseCase(songRepository)
    private val getSongProgressUseCase = GetSongProgressUseCase(songRepository)
    private val saveSongProgressUseCase = SaveSongProgressUseCase(songRepository)
    private val filterSongsUseCase = FilterSongsUseCase()
    private val prepareLessonUseCase = PrepareLessonUseCase()
    private val getSongTuningUseCase = GetSongTuningUseCase(tuningRepository)
    private val analyzeTuningUseCase = AnalyzeTuningUseCase()
    private val getTuningsUseCase = GetTuningsUseCase(tuningRepository)

    val homeViewModelFactory = HomeViewModelFactory(
        observeSongsUseCase = observeSongsUseCase,
        filterSongsUseCase = filterSongsUseCase,
        setFavoriteUseCase = setFavoriteUseCase
    )

    val songViewModelFactory = SongViewModelFactory(
        getSongUseCase = getSongUseCase,
        prepareLessonUseCase = prepareLessonUseCase,
        getSongProgressUseCase = getSongProgressUseCase,
        saveSongProgressUseCase = saveSongProgressUseCase,
        audioPlayer = audioPlayer,
        noteEvents = instrumentRepository.noteEvents,
        connectionState = instrumentRepository.connectionState,
        lessonControllerFactory = InstrumentLessonController.Factory(instrumentRepository),
        instrumentState = instrumentRepository.instrumentState,
        configurationKnown = instrumentRepository.configurationKnown,
        getSongTuningUseCase = getSongTuningUseCase,
        analyzeTuningUseCase = analyzeTuningUseCase
    )

    val settingsViewModelFactory = SettingsViewModelFactory(settingsRepository)
    val bluetoothViewModelFactory = BluetoothViewModelFactory(instrumentRepository)
    val harpViewModelFactory = HarpViewModelFactory(
        instrumentRepository = instrumentRepository,
        getSongUseCase = getSongUseCase,
        getSongTuningUseCase = getSongTuningUseCase,
        analyzeTuningUseCase = analyzeTuningUseCase,
        getTuningsUseCase = getTuningsUseCase
    )
}
