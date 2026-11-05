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

sealed interface InstrumentEvent {
    data class BatteryUpdated(
        val percent: Int,
        val millivolts: Int,
        val flags: Int
    ) : InstrumentEvent

    data class ConfigurationUpdated(
        val octaveShift: Int,
        val lever: List<Int>,
        val baseOctave: Int
    ) : InstrumentEvent

    data class ErrorReported(
        val code: Int,
        val referenceSequence: Int,
        val referenceType: Int
    ) : InstrumentEvent
}

data class NotePlayed(
    val stringId: Int,
    val midi: Int,
    val timestampMs: Int
) : InstrumentEvent

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
    ) : InstrumentCommand

    data object EndLesson : InstrumentCommand
}
