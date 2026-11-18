package com.example.harpapp.data.media

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
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
        pendingStart = false
        shouldResumeAfterFocusGain = false
        completionCallback = null
        abandonAudioFocus()

        val player = mediaPlayer
        mediaPlayer = null
        currentAssetPath = null
        player?.release()

        _playbackState.value = AudioPlaybackState.Idle
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
            return false
        }

        return try {
            player.start()
            shouldResumeAfterFocusGain = false
            _playbackState.value = AudioPlaybackState.Playing(assetPath)
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
            abandonAudioFocus()
            _playbackState.value = AudioPlaybackState.Paused(assetPath)
        } catch (exception: IllegalStateException) {
            handleError(assetPath, exception, player)
        }
    }

    private fun handleCompletion(player: MediaPlayer, assetPath: String) {
        if (mediaPlayer !== player) return

        pendingStart = false
        shouldResumeAfterFocusGain = false
        abandonAudioFocus()

        try {
            player.seekTo(0)
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
        pendingStart = false
        shouldResumeAfterFocusGain = false
        abandonAudioFocus()

        if (mediaPlayer === player) {
            mediaPlayer = null
            currentAssetPath = null
        }
        player.release()

        _playbackState.value = AudioPlaybackState.Error(assetPath, exception)
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
    }
}
