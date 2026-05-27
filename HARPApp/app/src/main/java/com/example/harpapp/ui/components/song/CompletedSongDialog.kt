package com.example.harpapp.ui.components.song

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun CompletedSongDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Congratulations!")
        },
        text = {
            Text("Song complete! Great job!")
        },
        confirmButton = {
            Button(
                onClick = onDismiss
            ) {
                Text("OK")
            }
        }
    )
}

@Preview
@Composable
fun CompletedSongDialogPreview() {
    CompletedSongDialog(onDismiss = {})
}