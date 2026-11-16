package com.example.harpapp.presentation.ui.components.harp

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.harpapp.domain.model.Accidental
import com.example.harpapp.domain.model.InstrumentState
import com.example.harpapp.domain.model.NoteName

@Composable
fun LeverButtons(
    tuning: InstrumentState,
    selectedNote: NoteName?,
    onNoteClick: (NoteName) -> Unit,
    highlight: Map<NoteName, Accidental> = emptyMap()
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top
    ) {
        NoteName.entries.forEach { name ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val target = highlight[name]
                LeverButton(
                    name = name,
                    position = tuning.lever(name),
                    selected = name == selectedNote,
                    highlighted = target != null,
                    onClick = { onNoteClick(name) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(1f)
                )
                target?.let { TargetTab(it) }
            }
        }
    }
}

@Composable
private fun TargetTab(target: Accidental) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TAB_INSET)
            .tuckedUnder(TAB_OVERLAP),
        shape = RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp),
        color = colors.error.copy(alpha = 0.12f).compositeOver(colors.background),
        contentColor = colors.error,
        border = BorderStroke(1.dp, colors.error.copy(alpha = 0.5f))
    ) {
        Text(
            text = "→ ${accidentalSymbol(target)}",
            modifier = Modifier.padding(top = TAB_OVERLAP + 2.dp, bottom = 4.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

private fun Modifier.tuckedUnder(overlap: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val overlapPx = overlap.roundToPx()
    layout(placeable.width, placeable.height - overlapPx) {
        placeable.place(0, -overlapPx)
    }
}

private val TAB_INSET = 3.dp
private val TAB_OVERLAP = 12.dp

@Composable
private fun LeverButton(
    name: NoteName,
    position: Accidental,
    selected: Boolean,
    highlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val tuningBackground by animateColorAsState(
        targetValue = tuningColor(position).copy(alpha = 0.2f).compositeOver(colors.background),
        label = "leverBackground"
    )

    Surface(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) colors.primary else tuningBackground,
        contentColor = if (selected) colors.onPrimary else colors.primary,
        border = if (highlighted) BorderStroke(3.dp, colors.error) else BorderStroke(1.dp, colors.primary)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = name.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = accidentalSymbol(position),
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1
            )
        }
    }
}

internal fun accidentalSymbol(accidental: Accidental): String = when (accidental) {
    Accidental.FLAT -> "♭"
    Accidental.NATURAL -> "♮"
    Accidental.SHARP -> "♯"
}
