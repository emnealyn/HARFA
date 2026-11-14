package com.example.harpapp.domain.usecase

import com.example.harpapp.domain.model.ConnectionState
import com.example.harpapp.domain.model.InstrumentCapabilities
import com.example.harpapp.domain.model.InstrumentCommand
import com.example.harpapp.domain.model.InstrumentState
import com.example.harpapp.domain.model.LessonProgress
import com.example.harpapp.domain.model.LessonStep
import com.example.harpapp.domain.model.ProgressUpdate
import com.example.harpapp.domain.model.SessionId
import com.example.harpapp.domain.model.StaleSessionException
import com.example.harpapp.domain.model.StepOutcome
import com.example.harpapp.domain.repository.InstrumentRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Przekłada przebieg lekcji na komendy LESSON_* dla harfy.
 */
class InstrumentLessonController private constructor(
    private val repository: InstrumentRepository,
    scope: CoroutineScope,
    private val onError: (Throwable) -> Unit
) {
    private sealed interface Signal {
        val session: SessionId?

        data class Start(val progress: LessonProgress, override val session: SessionId?) : Signal
        data class Resume(val progress: LessonProgress, override val session: SessionId?) : Signal
        data class Update(val update: ProgressUpdate, override val session: SessionId?) : Signal
        data class Pause(override val session: SessionId?) : Signal
        data class End(override val session: SessionId?) : Signal
        data class TuningChanged(val state: InstrumentState, override val session: SessionId?) : Signal
        data object SessionLost : Signal {
            override val session: SessionId? = null
        }
    }

    private val signals = Channel<Signal>(Channel.UNLIMITED)

    private var activeSession: SessionId? = null
    private var lessonSession: SessionId? = null
    private var paused = false
    private var litStep: LessonStep? = null
    private var litMask: Int = 0

    init {
        scope.launch {
            for (signal in signals) handle(signal)
        }
        scope.launch {
            repository.instrumentState.distinctUntilChanged().collect {
                enqueue { session -> Signal.TuningChanged(it, session) }
            }
        }
        scope.launch {
            repository.connectionState.distinctUntilChanged().collect {
                if (it != ConnectionState.CONNECTED) signals.trySend(Signal.SessionLost)
            }
        }
    }

    /** Nowa lekcja: LESSON_START i pierwszy LESSON_STEP. */
    fun start(progress: LessonProgress) {
        enqueue { Signal.Start(progress, it) }
    }

    /** Wynik zagrania: LESSON_RESULT, a po trafieniu także kolejny LESSON_STEP. */
    fun onUpdate(update: ProgressUpdate) {
        if (update.outcome != StepOutcome.IGNORED) enqueue { Signal.Update(update, it) }
    }

    /**
     * Wznowienie lekcji. W tej samej sesji, w której harfa ma wstrzymaną lekcję, wystarczy
     * `LESSON_PAUSE(0)`. W nowej sesji (po ponownym połączeniu) trzeba wysłać `LESSON_START`
     * i bieżący `LESSON_STEP` (spec §8 pkt 9).
     */
    fun resume(progress: LessonProgress) {
        if (!progress.isCompleted) enqueue { Signal.Resume(progress, it) }
    }

    fun pause() {
        enqueue { Signal.Pause(it) }
    }

    fun end() {
        enqueue { Signal.End(it) }
    }

    private inline fun enqueue(build: (SessionId?) -> Signal) {
        signals.trySend(build(repository.currentSession.value))
    }

    private suspend fun handle(signal: Signal) {
        if (signal == Signal.SessionLost) {
            clearHarpState()
            return
        }

        val current = repository.currentSession.value
        if (current != activeSession) {
            clearHarpState()
            activeSession = current
        }
        val session = signal.session
        if (session == null || session != current) return

        val commands: List<InstrumentCommand> = when (signal) {
            is Signal.Start -> startCommands(signal.progress, session)

            is Signal.Resume ->
                if (lessonSession == session && paused) resumeCommands(signal.progress)
                else startCommands(signal.progress, session)

            is Signal.Update -> when (signal.update.outcome) {
                StepOutcome.HIT -> listOfNotNull(
                    InstrumentCommand.LessonResult(InstrumentCommand.LessonResult.HIT),
                    stepCommand(signal.update.progress.currentStep)
                )
                StepOutcome.MISS ->
                    listOf(InstrumentCommand.LessonResult(InstrumentCommand.LessonResult.MISS))
                StepOutcome.COMPLETE -> {
                    litStep = null
                    listOf(InstrumentCommand.LessonResult(InstrumentCommand.LessonResult.COMPLETE))
                }
                StepOutcome.IGNORED -> emptyList()
            }

            is Signal.Pause ->
                if (lessonSession != session || paused) {
                    emptyList()
                } else {
                    paused = true
                    listOf(InstrumentCommand.PauseLesson(paused = true))
                }

            is Signal.End -> {
                litStep = null
                lessonSession = null
                paused = false
                listOf(InstrumentCommand.EndLesson)
            }

            is Signal.TuningChanged -> {
                val step = litStep
                if (paused || step == null || maskFor(step, signal.state) == litMask) emptyList()
                else listOfNotNull(stepCommand(step, signal.state))
            }

            Signal.SessionLost -> emptyList()
        }

        val capabilities = repository.capabilities.first()
        for (command in commands) {
            val wireCommand = adaptTo(capabilities, command)
            if (wireCommand != null) {
                try {
                    repository.send(wireCommand, session)
                } catch (e: StaleSessionException) {
                    clearHarpState()
                    return
                } catch (e: CancellationException) {
                    if (!currentCoroutineContext().isActive) throw e

                    clearHarpState()
                    return
                } catch (e: Exception) {
                    onError(e)
                    return // pozostałe komendy tego sygnału nie mają sensu
                }
            }
            if (command is InstrumentCommand.LessonStep) litMask = command.mask
        }
    }

    private suspend fun startCommands(
        progress: LessonProgress,
        session: SessionId
    ): List<InstrumentCommand> {
        litStep = null
        lessonSession = session
        paused = false
        return listOfNotNull(
            InstrumentCommand.StartLesson(progress.lesson.steps.size),
            stepCommand(progress.currentStep)
        )
    }

    private suspend fun resumeCommands(progress: LessonProgress): List<InstrumentCommand> {
        paused = false
        val step = progress.currentStep
        val state = repository.instrumentState.first()
        val stepChanged = step != litStep || (step != null && maskFor(step, state) != litMask)
        return listOfNotNull(
            InstrumentCommand.PauseLesson(paused = false),
            if (stepChanged) stepCommand(step, state) else null
        )
    }

    private fun clearHarpState() {
        lessonSession = null
        paused = false
        litStep = null
        litMask = 0
    }

    private fun adaptTo(
        capabilities: InstrumentCapabilities,
        command: InstrumentCommand
    ): InstrumentCommand? = when {
        capabilities.lesson -> command
        !capabilities.leds -> null
        else -> when (command) {
            is InstrumentCommand.LessonStep ->
                InstrumentCommand.SetLeds(command.mask, InstrumentCommand.SetLeds.STEADY)
            is InstrumentCommand.LessonResult ->
                if (command.result == InstrumentCommand.LessonResult.COMPLETE) ledsOff() else null
            is InstrumentCommand.PauseLesson ->
                if (command.paused) ledsOff()
                else InstrumentCommand.SetLeds(litMask, InstrumentCommand.SetLeds.STEADY)
            InstrumentCommand.EndLesson -> ledsOff()
            else -> null
        }
    }

    private fun ledsOff() = InstrumentCommand.SetLeds(0, InstrumentCommand.SetLeds.STEADY)

    private suspend fun stepCommand(
        step: LessonStep?,
        state: InstrumentState? = null
    ): InstrumentCommand.LessonStep? {
        step ?: return null
        val mask = maskFor(step, state ?: repository.instrumentState.first())
        litStep = step
        return InstrumentCommand.LessonStep(index = step.index, mask = mask)
    }

    private fun maskFor(step: LessonStep, state: InstrumentState): Int =
        state.stringFor(step.pitch)?.let { 1 shl it.index } ?: 0

    class Factory(private val repository: InstrumentRepository) {
        fun create(
            scope: CoroutineScope,
            onError: (Throwable) -> Unit = {}
        ): InstrumentLessonController = InstrumentLessonController(repository, scope, onError)
    }
}