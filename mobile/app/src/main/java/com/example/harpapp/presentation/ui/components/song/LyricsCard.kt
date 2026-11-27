package com.example.harpapp.presentation.ui.components.song

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.harpapp.R
import com.example.harpapp.domain.model.LyricNote

@Composable
fun LyricsCard(
    lyrics: List<LyricNote>,
    currentNoteIndex: Int,
    learningComplete: Boolean,
    isPaused: Boolean,
    onPausedClick: () -> Unit,
    modifier: Modifier = Modifier,
    pausedByConnection: Boolean = false,
    fillHeight: Boolean = false,
    containerAlpha: Float = 0.3f,
    contentPadding: Dp = 16.dp,
    header: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = containerAlpha)
        )
    ) {
        if (header != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                header()
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        Box(
            modifier = if (fillHeight) {
                Modifier.fillMaxWidth().weight(1f)
            } else {
                Modifier.fillMaxWidth()
            }
        ) {
            val contentModifier = if (fillHeight) {
                Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .verticalScroll(rememberScrollState())
            } else {
                Modifier.padding(contentPadding)
            }
            Box(modifier = contentModifier) {
                LyricsDisplay(
                    lyrics = lyrics,
                    currentNoteIndex = currentNoteIndex,
                    learningComplete = learningComplete
                )
            }
            LessonPausedOverlay(
                visible = isPaused,
                pausedByConnection = pausedByConnection,
                onClick = onPausedClick,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

@Composable
private fun LessonPausedOverlay(
    visible: Boolean,
    pausedByConnection: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clickLabel = if (pausedByConnection) {
        stringResource(R.string.lesson_paused_connection_details)
    } else {
        stringResource(R.string.resume_learning)
    }
    val overlayText = if (pausedByConnection) {
        stringResource(R.string.lesson_paused_overlay_no_connection)
    } else {
        stringResource(R.string.lesson_paused_overlay)
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
                .clickable(
                    onClickLabel = clickLabel,
                    role = Role.Button,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            val iconSize = minOf(72.dp, maxHeight * 0.6f)
            val showText = maxHeight >= 140.dp

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.PauseCircle,
                    contentDescription = if (showText) null else overlayText,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(iconSize)
                )
                if (showText) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = overlayText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}
