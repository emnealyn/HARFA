package com.example.harpapp.ui.components.song

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.harpapp.model.LyricNote
import com.example.harpapp.model.NoteState

@Composable
fun NoteItem(
    item: LyricNote,
    state: NoteState,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val syllableColor = when (state) {
        NoteState.DONE -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
        NoteState.ACTIVE -> MaterialTheme.colorScheme.onSurface
        NoteState.UPCOMING -> MaterialTheme.colorScheme.onSurface
    }

    val noteColor = when (state) {
        NoteState.DONE -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        NoteState.ACTIVE -> MaterialTheme.colorScheme.primary
        NoteState.UPCOMING -> MaterialTheme.colorScheme.primaryContainer
    }

    val itemAlpha = if (state == NoteState.ACTIVE) pulseAlpha else 1f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .wrapContentWidth()
            .padding(horizontal = 8.dp)
            .alpha(if (state == NoteState.DONE) 0.4f else 1f)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(
                    if (state == NoteState.ACTIVE)
                        MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha)
                    else
                        Color.Transparent
                )
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = item.syllable ?: "",
            maxLines = 1,
            softWrap = false,
            color = syllableColor,
            fontWeight = if (state == NoteState.ACTIVE) FontWeight.Bold else FontWeight.Normal
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(
                    when (state) {
                        NoteState.ACTIVE -> MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha)
                        NoteState.DONE -> MaterialTheme.colorScheme.surfaceVariant
                        NoteState.UPCOMING -> Color.Transparent
                    }
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = item.note,
                maxLines = 1,
                softWrap = false,
                fontSize = 13.sp,
                fontWeight = if (state == NoteState.ACTIVE) FontWeight.ExtraBold else FontWeight.Normal,
                color = when (state) {
                    NoteState.ACTIVE -> MaterialTheme.colorScheme.onPrimary
                    NoteState.DONE -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    NoteState.UPCOMING -> noteColor
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}