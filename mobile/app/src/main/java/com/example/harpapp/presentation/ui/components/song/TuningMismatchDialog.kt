package com.example.harpapp.presentation.ui.components.song

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.harpapp.R
import com.example.harpapp.domain.model.Accidental
import com.example.harpapp.domain.model.InstrumentState
import com.example.harpapp.domain.model.LeverChange
import com.example.harpapp.domain.model.NoteName
import com.example.harpapp.domain.model.OctaveShift
import com.example.harpapp.domain.model.TuningAnalysis

@Composable
fun TuningMismatchDialog(
    analysis: TuningAnalysis,
    keyName: String?,
    onGoToHarp: () -> Unit,
    onStartAnyway: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tuningTitle(analysis)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.tuning_dialog_intro))
                songTuningDetails(analysis, keyName).forEach { line ->
                    Text(text = "• $line", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(onClick = onGoToHarp) {
                Text(stringResource(R.string.tuning_dialog_go_to_harp))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.tuning_dialog_cancel))
                }
                TextButton(onClick = onStartAnyway) {
                    Text(stringResource(R.string.tuning_dialog_start_anyway))
                }
            }
        }
    )
}

@Preview
@Composable
fun TuningMismatchDialogPreview() {
    TuningMismatchDialog(
        analysis = TuningAnalysis.NeedsChanges(
            levers = listOf(LeverChange(NoteName.F, Accidental.NATURAL, Accidental.SHARP)),
            octave = OctaveShift.UP,
            target = InstrumentState.DEFAULT
        ),
        keyName = "G-dur",
        onGoToHarp = {},
        onStartAnyway = {},
        onDismiss = {}
    )
}
