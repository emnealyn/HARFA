package com.example.harpapp.presentation.ui.components.harp

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.harpapp.R
import com.example.harpapp.domain.model.HarpLayout
import com.example.harpapp.domain.model.InstrumentState
import com.example.harpapp.domain.model.OctaveShift
import com.example.harpapp.domain.model.StringId
import kotlin.math.roundToInt

@Composable
fun OctaveShiftSection(tuning: InstrumentState, targetShift: OctaveShift? = null) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.harp_octave_shift_title))
        OctaveShiftSlider(
            current = tuning.octaveShift,
            baseOctave = tuning.baseOctave,
            targetShift = targetShift
        )
    }
}

@Composable
private fun OctaveShiftSlider(
    current: OctaveShift,
    baseOctave: Int,
    targetShift: OctaveShift?,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val stops = OctaveShift.entries
    val thumbPosition by animateFloatAsState(
        targetValue = stops.indexOf(current).toFloat(),
        label = "octaveShiftThumb"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val step = size.width / stops.size
                val start = step / 2
                val centerY = size.height / 2
                fun stopX(position: Float) = start + position * step

                drawLine(
                    color = colors.secondary,
                    start = Offset(stopX(0f), centerY),
                    end = Offset(stopX(stops.lastIndex.toFloat()), centerY),
                    strokeWidth = 12.dp.toPx(),
                    cap = StrokeCap.Round
                )
                stops.indices.forEach { index ->
                    drawCircle(
                        color = colors.onSecondary,
                        radius = 3.dp.toPx(),
                        center = Offset(stopX(index.toFloat()), centerY)
                    )
                }
                drawCircle(
                    color = colors.primary,
                    radius = 15.dp.toPx(),
                    center = Offset(stopX(thumbPosition), centerY)
                )
            }
            AlignedToStop(
                position = thumbPosition,
                stopCount = stops.size,
                modifier = Modifier.matchParentSize()
            ) {
                Text(
                    text = octaveShiftLabel(current),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onPrimary
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            stops.forEach { shift ->
                val active = shift == current
                val isTarget = shift == targetShift && !active
                val lowest = HarpLayout.basePitch(StringId.ALL.first(), baseOctave + shift.offset)
                val highest = HarpLayout.basePitch(StringId.ALL.last(), baseOctave + shift.offset)
                val range = stringResource(
                    R.string.harp_range,
                    "${lowest.name}${lowest.octave}",
                    "${highest.name}${highest.octave}"
                )
                Text(
                    text = if (isTarget) "→ $range" else range,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (active || isTarget) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        isTarget -> colors.error
                        active -> colors.primary
                        else -> colors.secondary
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AlignedToStop(
    position: Float,
    stopCount: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val width = constraints.maxWidth
        val placeable = measurables.single().measure(constraints.copy(minWidth = 0, minHeight = 0))
        val height = maxOf(placeable.height, constraints.minHeight)
        val step = width / stopCount.toFloat()
        val center = step / 2 + position * step
        val x = (center - placeable.width / 2f).roundToInt().coerceIn(0, maxOf(0, width - placeable.width))
        layout(width, height) {
            placeable.place(x, (height - placeable.height) / 2)
        }
    }
}

internal fun octaveShiftLabel(shift: OctaveShift): String = when (shift) {
    OctaveShift.DOWN -> "−1"
    OctaveShift.NONE -> "0"
    OctaveShift.UP -> "+1"
}
