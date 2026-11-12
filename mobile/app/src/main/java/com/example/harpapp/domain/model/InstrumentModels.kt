package com.example.harpapp.domain.model

data class InstrumentDevice(
    val name: String?,
    val address: String
)

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

enum class BatteryPowerState {
    DISCHARGING,
    CHARGING,
    EXTERNAL_POWER
}

data class BatteryStatus(
    val percent: Int?,
    val millivolts: Int?,
    val powerState: BatteryPowerState,
    val isLow: Boolean
)

/**
 * Zdarzenia raportowane przez harfę. Zagranie struny to [NoteEvent] (docs/domain-model.md §4),
 * które również implementuje ten interfejs.
 */
sealed interface InstrumentEvent {
    data class BatteryUpdated(
        val status: BatteryStatus
    ) : InstrumentEvent

    /** Nowe strojenie instrumentu (`CONFIG_STATE`, docs/domain-model.md §3). */
    data class ConfigurationUpdated(
        val state: InstrumentState
    ) : InstrumentEvent

    data class ErrorReported(
        val code: Int,
        val referenceSequence: Int,
        val referenceType: Int
    ) : InstrumentEvent
}

sealed interface InstrumentCommand {
    data class RequestState(
        val flags: Int
    ) : InstrumentCommand

    data class SetLeds(
        val mask: Int,
        val mode: Int
    ) : InstrumentCommand

    data class StartLesson(
        val totalSteps: Int
    ) : InstrumentCommand

    data class LessonStep(
        val index: Int,
        val mask: Int
    ) : InstrumentCommand

    data class LessonResult(
        val result: Int
    ) : InstrumentCommand {
        companion object {
            const val MISS = 0
            const val HIT = 1
            const val COMPLETE = 2
        }
    }

    data object EndLesson : InstrumentCommand

    data class PauseLesson(
        val paused: Boolean
    ) : InstrumentCommand
}
