package com.example.harpapp.presentation.ui.components.song

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.harpapp.R
import com.example.harpapp.domain.model.Pitch
import com.example.harpapp.domain.model.TuningAnalysis
import com.example.harpapp.presentation.ui.components.harp.accidentalSymbol
import com.example.harpapp.presentation.ui.components.harp.octaveShiftLabel

@Composable
fun tuningTitle(analysis: TuningAnalysis): String = when (analysis) {
    TuningAnalysis.Compatible -> stringResource(R.string.harp_target_ready)
    is TuningAnalysis.Conflict -> stringResource(R.string.tuning_conflict_title)
    is TuningAnalysis.OutOfRange -> stringResource(R.string.tuning_out_of_range_title)
    TuningAnalysis.Incompatible, is TuningAnalysis.NeedsChanges -> stringResource(R.string.tuning_warning_title)
}

@Composable
fun tuningDetails(analysis: TuningAnalysis): List<String> = when (analysis) {
    TuningAnalysis.Compatible -> emptyList()
    TuningAnalysis.Incompatible -> listOf(stringResource(R.string.tuning_incompatible))
    is TuningAnalysis.NeedsChanges -> listOfNotNull(
        analysis.levers.takeIf { it.isNotEmpty() }?.let { levers ->
            stringResource(
                R.string.tuning_change_levers,
                levers.joinToString { "${it.name.name}${accidentalSymbol(it.to)}" }
            )
        },
        analysis.octave?.let { stringResource(R.string.tuning_change_octave, octaveShiftLabel(it)) }
    )
    is TuningAnalysis.Conflict -> analysis.conflicts.map { conflict ->
        stringResource(R.string.tuning_conflict_item, conflict.name.name, conflict.pitches.joinToString())
    }
    is TuningAnalysis.OutOfRange -> listOf(
        stringResource(R.string.tuning_out_of_range_notes, analysis.pitches.joinToString())
    )
}

@Composable
fun songTuningDetails(analysis: TuningAnalysis, keyName: String?): List<String> {
    if (analysis !is TuningAnalysis.NeedsChanges || keyName == null) return tuningDetails(analysis)
    return listOfNotNull(
        analysis.levers.takeIf { it.isNotEmpty() }?.let { stringResource(R.string.tuning_change_key, keyName) },
        analysis.octave?.let { stringResource(R.string.tuning_change_octave_to, octaveShiftLabel(it)) }
    )
}

@Composable
fun TuningWarningBanner(
    analysis: TuningAnalysis,
    keyName: String?,
    onNavigateToHarp: () -> Unit,
    modifier: Modifier = Modifier
) {
    WarningSurface(
        title = tuningTitle(analysis),
        details = songTuningDetails(analysis, keyName),
        onNavigateToHarp = onNavigateToHarp,
        modifier = modifier
    )
}

@Composable
fun StepTuningHintBanner(
    pitch: Pitch,
    analysis: TuningAnalysis,
    keyName: String?,
    onNavigateToHarp: () -> Unit,
    modifier: Modifier = Modifier
) {
    WarningSurface(
        title = stringResource(R.string.tuning_step_hint, pitch.toString()),
        details = songTuningDetails(analysis, keyName),
        onNavigateToHarp = onNavigateToHarp,
        modifier = modifier
    )
}

@Composable
private fun WarningSurface(
    title: String,
    details: List<String>,
    onNavigateToHarp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                details.forEach { line ->
                    Text(text = line, style = MaterialTheme.typography.bodySmall)
                }
                TextButton(
                    onClick = onNavigateToHarp,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.tuning_go_to_harp))
                }
            }
        }
    }
}
