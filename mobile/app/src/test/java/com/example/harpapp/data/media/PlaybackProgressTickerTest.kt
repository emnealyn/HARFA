package com.example.harpapp.data.media

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackProgressTickerTest {

    @Test
    fun startRunsImmediatelyAndSchedulesNextTick() {
        val scheduler = FakeProgressPollingScheduler()
        var ticks = 0
        val ticker = PlaybackProgressTicker(
            scheduler = scheduler,
            onTick = { ticks++ },
            intervalMs = 100L
        )

        ticker.start()
        scheduler.runImmediate()

        assertEquals(1, ticks)
        assertEquals(1, scheduler.delayedTaskCount)
    }

    @Test
    fun stopRemovesPendingTicks() {
        val scheduler = FakeProgressPollingScheduler()
        var ticks = 0
        val ticker = PlaybackProgressTicker(
            scheduler = scheduler,
            onTick = { ticks++ },
            intervalMs = 100L
        )

        ticker.start()
        scheduler.runImmediate()
        ticker.stop()
        scheduler.runDelayed()

        assertEquals(1, ticks)
        assertEquals(0, scheduler.delayedTaskCount)
    }

    @Test
    fun startingAnActiveTickerDoesNotDuplicatePolling() {
        val scheduler = FakeProgressPollingScheduler()
        var ticks = 0
        val ticker = PlaybackProgressTicker(
            scheduler = scheduler,
            onTick = { ticks++ },
            intervalMs = 100L
        )

        ticker.start()
        ticker.start()
        scheduler.runImmediate()

        assertEquals(1, ticks)
        assertEquals(1, scheduler.delayedTaskCount)
    }

    @Test
    fun stoppingFromTickDoesNotScheduleAnotherTick() {
        val scheduler = FakeProgressPollingScheduler()
        lateinit var ticker: PlaybackProgressTicker
        ticker = PlaybackProgressTicker(
            scheduler = scheduler,
            onTick = { ticker.stop() },
            intervalMs = 100L
        )

        ticker.start()
        scheduler.runImmediate()

        assertEquals(0, scheduler.delayedTaskCount)
    }

    private class FakeProgressPollingScheduler : ProgressPollingScheduler {
        private val immediateTasks = ArrayDeque<Runnable>()
        private val delayedTasks = mutableListOf<Runnable>()

        val delayedTaskCount: Int
            get() = delayedTasks.size

        override fun post(task: Runnable) {
            immediateTasks.addLast(task)
        }

        override fun postDelayed(task: Runnable, delayMillis: Long) {
            delayedTasks += task
        }

        override fun removeCallbacks(task: Runnable) {
            immediateTasks.removeAll { it === task }
            delayedTasks.removeAll { it === task }
        }

        fun runImmediate() {
            immediateTasks.removeFirst().run()
        }

        fun runDelayed() {
            if (delayedTasks.isNotEmpty()) {
                delayedTasks.removeAt(0).run()
            }
        }
    }
}
