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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.harpapp.domain.model.Accidental
import com.example.harpapp.domain.model.InstrumentState
import com.example.harpapp.domain.model.NoteName

@Composable
fun LeverButtons(
    tuning: InstrumentState,
    selectedNote: NoteName?,
    onNoteClick: (NoteName) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        NoteName.entries.forEach { name ->
            LeverButton(
                name = name,
                position = tuning.lever(name),
                selected = name == selectedNote,
                onClick = { onNoteClick(name) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun LeverButton(
    name: NoteName,
    position: Accidental,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val tuningBackground by animateColorAsState(
        targetValue = tuningColor(position).copy(alpha = 0.2f),
        label = "leverBackground"
    )

    Surface(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) colors.primary else tuningBackground,
        contentColor = if (selected) colors.onPrimary else colors.primary,
        border = BorderStroke(1.dp, colors.primary)
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

private fun accidentalSymbol(accidental: Accidental): String = when (accidental) {
    Accidental.FLAT -> "♭"
    Accidental.NATURAL -> "♮"
    Accidental.SHARP -> "♯"
}
