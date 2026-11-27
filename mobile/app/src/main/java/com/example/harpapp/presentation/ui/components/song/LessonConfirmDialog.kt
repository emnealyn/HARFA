package com.example.harpapp.presentation.ui.components.song

import androidx.annotation.StringRes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.harpapp.R

enum class LessonConfirmation(
    @StringRes val titleRes: Int,
    @StringRes val messageRes: Int,
    @StringRes val confirmRes: Int
) {
    END(
        titleRes = R.string.end_lesson_dialog_title,
        messageRes = R.string.end_lesson_dialog_message,
        confirmRes = R.string.end_lesson_dialog_confirm
    ),
    RESTART(
        titleRes = R.string.restart_lesson_dialog_title,
        messageRes = R.string.restart_lesson_dialog_message,
        confirmRes = R.string.restart_lesson_dialog_confirm
    )
}

@Composable
fun LessonConfirmDialog(
    confirmation: LessonConfirmation,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(confirmation.titleRes)) },
        text = { Text(stringResource(confirmation.messageRes)) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(confirmation.confirmRes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.lesson_dialog_cancel))
            }
        }
    )
}

@Preview
@Composable
fun EndLessonDialogPreview() {
    LessonConfirmDialog(confirmation = LessonConfirmation.END, onConfirm = {}, onDismiss = {})
}

@Preview
@Composable
fun RestartLessonDialogPreview() {
    LessonConfirmDialog(confirmation = LessonConfirmation.RESTART, onConfirm = {}, onDismiss = {})
}
