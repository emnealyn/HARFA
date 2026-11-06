package com.example.harpapp.presentation.ui.components.song

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.harpapp.R
import com.example.harpapp.domain.model.CatalogIssue
import com.example.harpapp.domain.model.CatalogIssueReason

/** Informuje, dlaczego nie można rozpocząć nauki utworu (docs/domain-model.md §6). */
@Composable
fun LessonIssuesDialog(
    issues: List<CatalogIssue>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.lesson_issues_title))
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(stringResource(R.string.lesson_issues_intro))
                issues.forEach { issue ->
                    Text(
                        text = "• " + issue.describe(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        }
    )
}

@Composable
private fun CatalogIssue.describe(): String {
    // Numer kroku liczony od 1, tak jak widzi go użytkownik.
    val step = (stepIndex ?: 0) + 1
    val note = raw.orEmpty()
    return when (reason) {
        CatalogIssueReason.NO_STEPS -> stringResource(R.string.lesson_issue_no_steps)
        CatalogIssueReason.EMPTY -> stringResource(R.string.lesson_issue_empty, step)
        CatalogIssueReason.MALFORMED -> stringResource(R.string.lesson_issue_malformed, step, note)
        CatalogIssueReason.OUT_OF_RANGE -> stringResource(R.string.lesson_issue_out_of_range, step, note)
    }
}

@Preview
@Composable
fun LessonIssuesDialogPreview() {
    LessonIssuesDialog(
        issues = listOf(
            CatalogIssue(3, "", CatalogIssueReason.EMPTY),
            CatalogIssue(7, "H4", CatalogIssueReason.MALFORMED),
            CatalogIssue(12, "D5", CatalogIssueReason.OUT_OF_RANGE)
        ),
        onDismiss = {}
    )
}
