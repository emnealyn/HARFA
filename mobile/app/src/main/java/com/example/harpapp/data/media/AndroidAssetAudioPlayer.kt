package com.example.harpapp.data.media

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.example.harpapp.domain.repository.AudioPlayer
import java.io.IOException

class AndroidAssetAudioPlayer(
    context: Context
) : AudioPlayer {
    private val appContext = context.applicationContext
    private var mediaPlayer: MediaPlayer? = null
    private var currentAssetPath: String? = null

    override fun toggle(assetPath: String, onCompleted: () -> Unit): Boolean {
        val player = mediaPlayer
        if (player != null && player.isPlaying) {
            player.pause()
            return false
        }

        if (player == null || currentAssetPath != assetPath) {
            release()
            val newPlayer = try {
                appContext.assets.openFd(assetPath).use { descriptor ->
                    MediaPlayer().apply {
                        setDataSource(
                            descriptor.fileDescriptor,
                            descriptor.startOffset,
                            descriptor.length
                        )
                        prepare()
                    }
                }
            } catch (exception: IOException) {
                Log.e("AndroidAssetAudioPlayer", "Could not load audio asset $assetPath", exception)
                return false
            } catch (exception: IllegalArgumentException) {
                Log.e("AndroidAssetAudioPlayer", "Invalid audio asset $assetPath", exception)
                return false
            }

            mediaPlayer = newPlayer
            currentAssetPath = assetPath
        }

        mediaPlayer?.setOnCompletionListener { completedPlayer ->
            completedPlayer.seekTo(0)
            onCompleted()
        }
        mediaPlayer?.start()
        return true
    }

    override fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
        currentAssetPath = null
    }
}
