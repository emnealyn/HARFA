package com.example.harpapp.data.media

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.harpapp.domain.model.AudioPlaybackProgress
import com.example.harpapp.domain.model.AudioPlaybackState
import com.example.harpapp.domain.repository.AudioPlayer
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidAssetAudioPlayer(
    context: Context
) : AudioPlayer {
    private val appContext = context.applicationContext
    private val audioManager = requireNotNull(
        appContext.getSystemService(AudioManager::class.java)
    )
    private val mainHandler = Handler(Looper.getMainLooper())
    private val progressTicker = PlaybackProgressTicker(
        scheduler = HandlerProgressPollingScheduler(mainHandler),
        onTick = ::updateCurrentPlaybackProgress,
        intervalMs = PROGRESS_UPDATE_INTERVAL_MS
    )
    private val audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        .setOnAudioFocusChangeListener(
            { focusChange -> handleAudioFocusChange(focusChange) },
            mainHandler
        )
        .setWillPauseWhenDucked(true)
        .build()

    private val _playbackState = MutableStateFlow<AudioPlaybackState>(
        AudioPlaybackState.Idle
    )
    override val playbackState: StateFlow<AudioPlaybackState> = _playbackState.asStateFlow()

    private val _playbackProgress = MutableStateFlow(AudioPlaybackProgress.EMPTY)
    override val playbackProgress: StateFlow<AudioPlaybackProgress> =
        _playbackProgress.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var currentAssetPath: String? = null
    private var completionCallback: (() -> Unit)? = null
    private var pendingStart = false
    private var shouldResumeAfterFocusGain = false
    private var audioFocusHeld = false

    override fun toggle(assetPath: String, onCompleted: () -> Unit): Boolean {
        completionCallback = onCompleted

        val player = mediaPlayer
        if (player != null && currentAssetPath == assetPath) {
            return when (playbackState.value) {
                is AudioPlaybackState.Playing -> {
                    pauseByUser(player, assetPath)
                    false
                }

                is AudioPlaybackState.Preparing -> {
                    release()
                    false
                }

                is AudioPlaybackState.Paused,
                is AudioPlaybackState.Error -> startPrepared(player, assetPath)

                AudioPlaybackState.Idle -> {
                    release()
                    completionCallback = onCompleted
                    prepare(assetPath)
                    true
                }
            }
        }

        release()
        completionCallback = onCompleted
        prepare(assetPath)
        return true
    }

    override fun release() {
        progressTicker.stop()
        pendingStart = false
        shouldResumeAfterFocusGain = false
        completionCallback = null
        abandonAudioFocus()

        val player = mediaPlayer
        mediaPlayer = null
        currentAssetPath = null
        player?.release()

        _playbackState.value = AudioPlaybackState.Idle
        _playbackProgress.value = AudioPlaybackProgress.EMPTY
    }

    override fun seekTo(positionMs: Long): Boolean {
        val player = mediaPlayer ?: return false
        val assetPath = currentAssetPath ?: return false
        if (playbackState.value !is AudioPlaybackState.Playing &&
            playbackState.value !is AudioPlaybackState.Paused
        ) {
            return false
        }

        val durationMs = _playbackProgress.value.durationMs
        val targetPositionMs = if (durationMs > 0L) {
            positionMs.coerceIn(0L, durationMs)
        } else {
            positionMs.coerceAtLeast(0L)
        }

        return try {
            player.seekTo(targetPositionMs.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
            _playbackProgress.value = _playbackProgress.value.copy(
                positionMs = targetPositionMs
            )
            true
        } catch (exception: IllegalStateException) {
            handleError(assetPath, exception, player)
            false
        }
    }

    private fun prepare(assetPath: String) {
        val player = MediaPlayer()
        try {
            appContext.assets.openFd(assetPath).use { descriptor ->
                player.setDataSource(
                    descriptor.fileDescriptor,
                    descriptor.startOffset,
                    descriptor.length
                )
            }

            player.setOnPreparedListener { preparedPlayer ->
                handlePrepared(preparedPlayer, assetPath)
            }
            player.setOnCompletionListener { completedPlayer ->
                handleCompletion(completedPlayer, assetPath)
            }
            player.setOnErrorListener { failedPlayer, what, extra ->
                handleError(
                    assetPath = assetPath,
                    exception = IllegalStateException(
                        "MediaPlayer error: what=$what, extra=$extra"
                    ),
                    player = failedPlayer
                )
                true
            }

            mediaPlayer = player
            currentAssetPath = assetPath
            pendingStart = true
            _playbackState.value = AudioPlaybackState.Preparing(assetPath)
            player.prepareAsync()
        } catch (exception: IOException) {
            handleError(assetPath, exception, player)
        } catch (exception: IllegalArgumentException) {
            handleError(assetPath, exception, player)
        } catch (exception: IllegalStateException) {
            handleError(assetPath, exception, player)
        }
    }

    private fun handlePrepared(player: MediaPlayer, assetPath: String) {
        if (mediaPlayer !== player) {
            player.release()
            return
        }

        updatePlaybackProgress(player)
        if (!pendingStart) {
            _playbackState.value = AudioPlaybackState.Paused(assetPath)
            return
        }

        pendingStart = false
        startPrepared(player, assetPath)
    }

    private fun startPrepared(player: MediaPlayer, assetPath: String): Boolean {
        if (mediaPlayer !== player) return false

        if (!requestAudioFocus()) {
            _playbackState.value = AudioPlaybackState.Error(
                assetPath = assetPath,
                cause = IllegalStateException("Audio focus was not granted")
            )
            _playbackProgress.value = AudioPlaybackProgress.EMPTY
            return false
        }

        return try {
            player.start()
            shouldResumeAfterFocusGain = false
            _playbackState.value = AudioPlaybackState.Playing(assetPath)
            updatePlaybackProgress(player)
            progressTicker.start()
            true
        } catch (exception: IllegalStateException) {
            handleError(assetPath, exception, player)
            false
        }
    }

    private fun pauseByUser(player: MediaPlayer, assetPath: String) {
        pendingStart = false
        shouldResumeAfterFocusGain = false

        try {
            player.pause()
            progressTicker.stop()
            abandonAudioFocus()
            updatePlaybackProgress(player)
            _playbackState.value = AudioPlaybackState.Paused(assetPath)
        } catch (exception: IllegalStateException) {
            handleError(assetPath, exception, player)
        }
    }

    private fun handleCompletion(player: MediaPlayer, assetPath: String) {
        if (mediaPlayer !== player) return

        progressTicker.stop()
        pendingStart = false
        shouldResumeAfterFocusGain = false
        abandonAudioFocus()

        try {
            player.seekTo(0)
            updatePlaybackProgress(player)
            _playbackState.value = AudioPlaybackState.Paused(assetPath)
            completionCallback?.invoke()
        } catch (exception: IllegalStateException) {
            handleError(assetPath, exception, player)
        }
    }

    private fun handleError(
        assetPath: String,
        exception: Throwable,
        player: MediaPlayer
    ) {
        Log.e(TAG, "Could not play audio asset $assetPath", exception)
        progressTicker.stop()
        pendingStart = false
        shouldResumeAfterFocusGain = false
        abandonAudioFocus()

        if (mediaPlayer === player) {
            mediaPlayer = null
            currentAssetPath = null
        }
        player.release()

        _playbackState.value = AudioPlaybackState.Error(assetPath, exception)
        _playbackProgress.value = AudioPlaybackProgress.EMPTY
    }

    private fun updatePlaybackProgress(player: MediaPlayer) {
        _playbackProgress.value = AudioPlaybackProgress(
            positionMs = player.currentPosition.toLong().coerceAtLeast(0L),
            durationMs = player.duration.toLong().coerceAtLeast(0L)
        )
    }

    private fun updateCurrentPlaybackProgress() {
        if (_playbackState.value !is AudioPlaybackState.Playing) return
        mediaPlayer?.let(::updatePlaybackProgress)
    }

    private fun handleAudioFocusChange(focusChange: Int) {
        val player = mediaPlayer ?: return
        val assetPath = currentAssetPath ?: return

        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (shouldResumeAfterFocusGain &&
                    playbackState.value is AudioPlaybackState.Paused
                ) {
                    startPrepared(player, assetPath)
                }
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (playbackState.value is AudioPlaybackState.Playing) {
                    shouldResumeAfterFocusGain = true
                    try {
                        player.pause()
                        progressTicker.stop()
                        updatePlaybackProgress(player)
                        _playbackState.value = AudioPlaybackState.Paused(assetPath)
                    } catch (exception: IllegalStateException) {
                        handleError(assetPath, exception, player)
                    }
                }
            }

            AudioManager.AUDIOFOCUS_LOSS -> {
                release()
            }
        }
    }

    private fun requestAudioFocus(): Boolean {
        if (audioFocusHeld) return true

        audioFocusHeld =
            audioManager.requestAudioFocus(audioFocusRequest) ==
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return audioFocusHeld
    }

    private fun abandonAudioFocus() {
        if (!audioFocusHeld) return

        audioManager.abandonAudioFocusRequest(audioFocusRequest)
        audioFocusHeld = false
    }

    private companion object {
        const val TAG = "AndroidAssetAudioPlayer"
        const val PROGRESS_UPDATE_INTERVAL_MS = 100L
    }
}
