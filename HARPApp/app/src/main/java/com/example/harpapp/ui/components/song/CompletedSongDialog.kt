package com.example.harpapp.ui.components.song

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.harpapp.R

@Composable
fun CompletedSongDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.congratulations))
        },
        text = {
            Text(stringResource(R.string.song_complete_great_job))
        },
        confirmButton = {
            Button(
                onClick = onDismiss
            ) {
                Text(stringResource(R.string.ok))
            }
        }
    )
}

@Preview
@Composable
fun CompletedSongDialogPreview() {
    CompletedSongDialog(onDismiss = {})
}