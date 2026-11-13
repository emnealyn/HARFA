package com.example.harpapp.domain.usecase

import com.example.harpapp.domain.model.Accidental
import com.example.harpapp.domain.model.HarpLayout
import com.example.harpapp.domain.model.InstrumentState
import com.example.harpapp.domain.model.LeverChange
import com.example.harpapp.domain.model.NoteConflict
import com.example.harpapp.domain.model.NoteName
import com.example.harpapp.domain.model.OctaveShift
import com.example.harpapp.domain.model.Pitch
import com.example.harpapp.domain.model.PitchParseResult
import com.example.harpapp.domain.model.Song
import com.example.harpapp.domain.model.StringId
import com.example.harpapp.domain.model.Tuning
import com.example.harpapp.domain.model.TuningAnalysis

class AnalyzeTuningUseCase {

    operator fun invoke(song: Song, current: InstrumentState, suggested: Tuning?): TuningAnalysis =
        invoke(song.pitches(), current, suggested)

    operator fun invoke(
        pitches: Collection<Pitch>,
        current: InstrumentState,
        suggested: Tuning?
    ): TuningAnalysis {
        val distinct = pitches.distinctBy { it.midi }.sortedBy { it.midi }
        if (distinct.all { current.stringFor(it) != null }) return TuningAnalysis.Compatible

        val shifts = shiftOrder(current.octaveShift)
        val optionsByShift = shifts.associateWith { shift ->
            distinct.associateWith { optionsFor(it, shift, current.baseOctave) }
        }

        val reachableShifts = shifts.filter { shift ->
            optionsByShift.getValue(shift).values.none { it.isEmpty() }
        }
        if (reachableShifts.isEmpty()) {
            val best = shifts.minBy { shift -> optionsByShift.getValue(shift).values.count { it.isEmpty() } }
            val unreachable = optionsByShift.getValue(best).filterValues { it.isEmpty() }.keys
            return TuningAnalysis.OutOfRange(unreachable.toList())
        }

        val target = suggestedTarget(suggested, current, reachableShifts, optionsByShift)
            ?: return conflictOrIncompatible(reachableShifts, optionsByShift)

        return changesTowards(target, current, distinct)
    }

    private data class Option(val name: NoteName, val accidental: Accidental)

    private fun Song.pitches(): List<Pitch> =
        lyricsWithNotes.mapNotNull { (Pitch.parse(it.note) as? PitchParseResult.Valid)?.pitch }

    private fun shiftOrder(current: OctaveShift): List<OctaveShift> =
        (listOf(current, OctaveShift.NONE) + OctaveShift.entries).distinct()

    private fun optionsFor(pitch: Pitch, shift: OctaveShift, baseOctave: Int): List<Option> =
        StringId.ALL.mapNotNull { id ->
            val offset = pitch.midi - HarpLayout.basePitch(id, baseOctave).midi - shift.offset * 12
            Accidental.entries.firstOrNull { it.offset == offset }
                ?.let { Option(HarpLayout.noteNameOf(id), it) }
        }.distinct()

    private fun List<Accidental>.satisfies(options: List<Option>): Boolean =
        options.any { this[it.name.ordinal] == it.accidental }

    private fun List<Accidental>.isPlayable(options: Map<Pitch, List<Option>>): Boolean =
        options.values.all { satisfies(it) }

    private fun InstrumentState.leverList(): List<Accidental> = NoteName.entries.map { lever(it) }

    private fun suggestedTarget(
        suggested: Tuning?,
        current: InstrumentState,
        shifts: List<OctaveShift>,
        optionsByShift: Map<OctaveShift, Map<Pitch, List<Option>>>
    ): InstrumentState? {
        val retuned = suggested?.toInstrumentState(current) ?: return null
        val levers = retuned.leverList()
        val shift = shifts.firstOrNull { levers.isPlayable(optionsByShift.getValue(it)) } ?: return null
        return retuned.copy(octaveShift = shift)
    }

    private fun conflictOrIncompatible(
        shifts: List<OctaveShift>,
        optionsByShift: Map<OctaveShift, Map<Pitch, List<Option>>>
    ): TuningAnalysis {
        var bestOptions = optionsByShift.getValue(shifts.first())
        var bestLevers = ALL_LEVERS.first()
        var bestSatisfied = -1
        for (shift in shifts) {
            val options = optionsByShift.getValue(shift)
            for (levers in ALL_LEVERS) {
                val satisfied = options.values.count { levers.satisfies(it) }
                if (satisfied > bestSatisfied) {
                    bestOptions = options
                    bestLevers = levers
                    bestSatisfied = satisfied
                }
            }
        }

        val occupied = bestOptions.mapNotNull { (pitch, options) ->
            options.firstOrNull { bestLevers[it.name.ordinal] == it.accidental }?.let { pitch to it.name }
        }.toMap()
        val unsatisfied = bestOptions.filterKeys { it !in occupied }.map { (pitch, options) ->
            pitch to (options.firstOrNull { it.name == pitch.name } ?: options.first()).name
        }
        if (unsatisfied.isEmpty()) return TuningAnalysis.Incompatible

        val conflicts = unsatisfied.groupBy({ it.second }, { it.first }).map { (name, blocked) ->
            val holders = occupied.filterValues { it == name }.keys
            NoteConflict(name, (holders + blocked).distinct().sortedBy { it.midi })
        }.sortedBy { it.name }
        return TuningAnalysis.Conflict(conflicts)
    }

    private fun changesTowards(
        target: InstrumentState,
        current: InstrumentState,
        pitches: List<Pitch>
    ): TuningAnalysis {
        val usedNames = pitches.mapNotNull { target.stringFor(it) }.map { HarpLayout.noteNameOf(it) }.toSet()
        val leverChanges = NoteName.entries
            .filter { it in usedNames && target.lever(it) != current.lever(it) }
            .map { LeverChange(it, current.lever(it), target.lever(it)) }
        val octave = target.octaveShift.takeIf { it != current.octaveShift }
        if (leverChanges.isEmpty() && octave == null) return TuningAnalysis.Compatible

        return TuningAnalysis.NeedsChanges(leverChanges, octave)
    }

    private companion object {
        val ALL_LEVERS: List<List<Accidental>> =
            NoteName.entries.fold(listOf(emptyList())) { partial, _ ->
                partial.flatMap { prefix -> Accidental.entries.map { prefix + it } }
            }
    }
}
