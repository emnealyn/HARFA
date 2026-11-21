package com.example.harpapp.data.media

import android.os.Handler

internal interface ProgressPollingScheduler {
    fun post(task: Runnable)

    fun postDelayed(task: Runnable, delayMillis: Long)

    fun removeCallbacks(task: Runnable)
}

internal class HandlerProgressPollingScheduler(
    private val handler: Handler
) : ProgressPollingScheduler {
    override fun post(task: Runnable) {
        handler.post(task)
    }

    override fun postDelayed(task: Runnable, delayMillis: Long) {
        handler.postDelayed(task, delayMillis)
    }

    override fun removeCallbacks(task: Runnable) {
        handler.removeCallbacks(task)
    }
}

internal class PlaybackProgressTicker(
    private val scheduler: ProgressPollingScheduler,
    private val onTick: () -> Unit,
    private val intervalMs: Long
) {
    private var running = false

    private val tick = object : Runnable {
        override fun run() {
            if (!running) return

            onTick()
            if (running) {
                scheduler.postDelayed(this, intervalMs)
            }
        }
    }

    fun start() {
        if (running) return

        running = true
        scheduler.removeCallbacks(tick)
        scheduler.post(tick)
    }

    fun stop() {
        if (!running) return

        running = false
        scheduler.removeCallbacks(tick)
    }
}
