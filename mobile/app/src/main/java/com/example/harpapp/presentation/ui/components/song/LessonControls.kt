package com.example.harpapp.presentation.ui.components.song

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.harpapp.R

@Composable
fun LessonControls(
    isLearningMode: Boolean,
    learningComplete: Boolean,
    canResumeLearning: Boolean,
    showResumeChoices: Boolean,
    isLessonPaused: Boolean,
    canToggleLessonPause: Boolean,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onResumeClick: () -> Unit,
    onRestartClick: () -> Unit,
    onPauseToggleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            !isLearningMode && canResumeLearning -> {
                ResumeButton(onClick = onResumeClick)
                RestartButton(onClick = onRestartClick)
            }
            !isLearningMode -> LessonActionButton(
                label = stringResource(R.string.start_learning),
                icon = Icons.Filled.PlayCircle,
                onClick = onStartClick,
                isPrimary = true
            )
            showResumeChoices -> {
                ResumeButton(onClick = onResumeClick)
                StopButton(onClick = onStopClick)
                RestartIconButton(onClick = onRestartClick)
            }
            learningComplete -> StopButton(onClick = onStopClick)
            else -> {
                LessonActionButton(
                    label = if (isLessonPaused) stringResource(R.string.resume_learning_short)
                    else stringResource(R.string.pause_learning),
                    icon = if (isLessonPaused) Icons.Filled.PlayCircle else Icons.Filled.PauseCircle,
                    onClick = onPauseToggleClick,
                    isPrimary = true,
                    enabled = canToggleLessonPause
                )
                StopButton(onClick = onStopClick)
                RestartIconButton(onClick = onRestartClick)
            }
        }
    }
}

@Composable
private fun RowScope.ResumeButton(onClick: () -> Unit) {
    LessonActionButton(
        label = stringResource(R.string.resume_learning_short),
        icon = Icons.Filled.PlayCircle,
        onClick = onClick,
        isPrimary = true
    )
}

@Composable
private fun RowScope.RestartButton(onClick: () -> Unit) {
    LessonActionButton(
        label = stringResource(R.string.restart_learning_short),
        icon = Icons.Filled.RestartAlt,
        onClick = onClick,
        isPrimary = false
    )
}

@Composable
private fun RowScope.StopButton(onClick: () -> Unit) {
    LessonActionButton(
        label = stringResource(R.string.stop_learning),
        icon = Icons.Filled.Stop,
        onClick = onClick,
        isPrimary = false
    )
}

@Composable
private fun RestartIconButton(onClick: () -> Unit) {
    OutlinedIconButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Icon(
            imageVector = Icons.Filled.RestartAlt,
            contentDescription = stringResource(R.string.restart_learning_short),
            modifier = Modifier.size(ButtonDefaults.IconSize)
        )
    }
}

private val LessonButtonPadding =PaddingValues(horizontal = 14.dp, vertical = 8.dp)

@Composable
private fun RowScope.LessonActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    isPrimary: Boolean,
    enabled: Boolean = true
) {
    val buttonModifier = Modifier.weight(1f, fill = false)
    val content: @Composable RowScope.() -> Unit = {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize)
        )
        Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }

    if (isPrimary) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = buttonModifier,
            shape = MaterialTheme.shapes.medium,
            contentPadding = LessonButtonPadding,
            content = content
        )
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = buttonModifier,
            shape = MaterialTheme.shapes.medium,
            contentPadding = LessonButtonPadding,
            content = content
        )
    }
}
