package com.example.harpapp.domain.usecase

import com.example.harpapp.domain.model.ConnectionState
import com.example.harpapp.domain.model.InstrumentCommand
import com.example.harpapp.domain.model.InstrumentState
import com.example.harpapp.domain.model.LessonProgress
import com.example.harpapp.domain.model.LessonStep
import com.example.harpapp.domain.model.ProgressUpdate
import com.example.harpapp.domain.model.StepOutcome
import com.example.harpapp.domain.repository.InstrumentRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
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
        data class Start(val progress: LessonProgress) : Signal
        data class Update(val update: ProgressUpdate) : Signal
        data object End : Signal
        data class TuningChanged(val state: InstrumentState) : Signal
        data object SessionLost : Signal
    }

    private val signals = Channel<Signal>(Channel.UNLIMITED)

    private var litStep: LessonStep? = null
    private var litMask: Int = 0

    init {
        scope.launch {
            for (signal in signals) handle(signal)
        }
        scope.launch {
            repository.instrumentState.distinctUntilChanged().collect {
                signals.trySend(Signal.TuningChanged(it))
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
        signals.trySend(Signal.Start(progress))
    }

    /** Wynik zagrania: LESSON_RESULT, a po trafieniu także kolejny LESSON_STEP. */
    fun onUpdate(update: ProgressUpdate) {
        if (update.outcome != StepOutcome.IGNORED) signals.trySend(Signal.Update(update))
    }

    /**
     * Wznowienie po ponownym połączeniu. Spec §6 pkt 9: w nowej sesji trzeba wysłać
     * LESSON_START i ostatni nieukończony LESSON_STEP.
     */
    fun resume(progress: LessonProgress) {
        if (!progress.isCompleted) signals.trySend(Signal.Start(progress))
    }

    /**
     * Ręczna pauza. Protokół nie ma osobnej komendy pauzy, więc lekcja na harfie jest kończona
     * (`LESSON_END` gasi diody). Wznowienie przez [resume] wysyła `LESSON_START` i bieżący krok.
     */
    fun pause() {
        signals.trySend(Signal.End)
    }

    fun end() {
        signals.trySend(Signal.End)
    }

    private suspend fun handle(signal: Signal) {
        if (signal == Signal.SessionLost) {
            litStep = null
            return
        }
        if (repository.connectionState.first() != ConnectionState.CONNECTED) return

        val commands: List<InstrumentCommand> = when (signal) {
            is Signal.Start -> {
                litStep = null
                listOfNotNull(
                    InstrumentCommand.StartLesson(signal.progress.lesson.steps.size),
                    stepCommand(signal.progress.currentStep)
                )
            }

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

            Signal.End -> {
                litStep = null
                listOf(InstrumentCommand.EndLesson)
            }

            is Signal.TuningChanged -> {
                val step = litStep
                if (step == null || maskFor(step, signal.state) == litMask) emptyList()
                else listOfNotNull(stepCommand(step, signal.state))
            }

            Signal.SessionLost -> emptyList()
        }

        for (command in commands) {
            try {
                repository.send(command)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onError(e)
                return // pozostałe komendy tego sygnału nie mają sensu
            }
            if (command is InstrumentCommand.LessonStep) litMask = command.mask
        }
    }

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