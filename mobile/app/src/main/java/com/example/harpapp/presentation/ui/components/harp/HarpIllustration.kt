package com.example.harpapp.presentation.ui.components.harp

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.Group
import androidx.compose.ui.graphics.vector.Path
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import com.example.harpapp.domain.model.Accidental
import com.example.harpapp.domain.model.HarpLayout
import com.example.harpapp.domain.model.InstrumentState
import com.example.harpapp.domain.model.StringId

private const val VIEWPORT_WIDTH = 156.6704f
private const val VIEWPORT_HEIGHT = 208.8939f
private const val OFFSET_X = -36.1717f
private const val OFFSET_Y = -30.7572f
private const val STRING_WIDTH = 2.1711f

private val framePath = addPathNodes(
    "m 58.48798,228.45097 c 0,0 36.534353,0.3572 55.94752,0.1729 32.55675,-0.30908 " +
    "-0.20227,-6.41881 0.48191,-8.06897 5.87918,-14.17979 -29.58063,1.14993 " +
    "-19.677583,-10.05325 6.425313,-7.2689 57.325193,-69.18325 84.642683,-103.91237 " +
    "4.6796,-5.94924 6.59086,-10.859385 7.76024,-27.069224 0.58571,-8.119088 " +
    "-1.07967,-17.044339 -5.15215,-20.370374 -7.48828,-6.115755 -11.96317,-4.351385 " +
    "-28.78738,1.04167 -34.36805,11.016784 -38.69834,-10.619657 -84.485413,-17.7814 " +
    "-6.052545,-0.946703 -14.91233,-1.702129 -20.78388,3.538301 -6.215714,5.5476 " +
    "-9.384459,16.434812 -5.432702,14.796864 26.530274,-10.996429 80.144355,19.957535 " +
    "97.579455,17.576145 8.91834,-1.218118 26.95068,-8.672757 31.13326,-2.649179 " +
    "2.79515,4.025465 -2.47365,21.801042 -5.53905,25.447177 -34.23302,40.7183 " +
    "-56.92606,61.05688 -100.517275,114.72487 -2.577319,3.1731 -1.429814,4.95554 " +
    "0.315357,6.63047 1.745172,1.67493 -7.484992,5.97637 -7.484992,5.97637 z"
)

private val stringsMask = addPathNodes(
    "M 43.0012,60.7451 C 69.5315,49.7487 123.1456,80.7027 140.5807,78.3213 C 149.499,77.1031 " +
    "167.5314,69.6485 171.7139,75.6721 C 174.5091,79.6975 169.2403,97.4731 166.1749,101.1193 C " +
    "131.9419,141.8376 109.2488,162.1761 65.6576,215.8441 Z"
)

private val stringPaths = listOf(
    "M 56.8305,53.5659 L 84.0556,195.7969",
    "M 62.6972,53.5659 L 89.9224,195.7969",
    "M 68.5639,53.5659 L 95.7891,195.7969",
    "M 74.4307,53.5659 L 101.6558,195.7969",
    "M 80.2974,53.5659 L 107.5226,195.7969",
    "M 86.1642,53.5659 L 113.3893,195.7969",
    "M 92.0309,53.5659 L 119.2561,195.7969",
    "M 97.8976,53.5659 L 125.1228,195.7969",
    "M 103.7644,53.5659 L 130.9895,195.7969",
    "M 109.6311,53.5659 L 136.8563,195.7969",
    "M 115.4979,53.5659 L 142.723,195.7969",
    "M 121.3646,53.5659 L 148.5897,195.7969",
    "M 127.2313,53.5659 L 154.4565,195.7969",
    "M 133.0981,53.5659 L 160.3232,195.7969",
    "M 138.9648,53.5659 L 166.19,195.7969"
).map(::addPathNodes)

@Composable
fun tuningColor(accidental: Accidental): Color {
    val colors = MaterialTheme.colorScheme
    return when (accidental) {
        Accidental.SHARP -> lerp(colors.error, Color.White, 0.35f)
        Accidental.FLAT -> lerp(colors.error, Color.Black, 0.4f)
        Accidental.NATURAL -> colors.secondary
    }
}

@Composable
fun HarpIllustration(
    tuning: InstrumentState?,
    selectedStrings: List<StringId>,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val selected = selectedStrings.map { it.index }.toSet()
    val stringColors = StringId.ALL.map { id ->
        val accidental = tuning?.lever(HarpLayout.noteNameOf(id)) ?: Accidental.NATURAL
        val base = tuningColor(accidental)
        animateColorAsState(
            targetValue = if (selected.isEmpty() || id.index in selected) base else base.copy(alpha = 0.2f),
            label = "harpStringColor${id.index}"
        ).value
    }
    val stringWidths = StringId.ALL.map { id ->
        animateFloatAsState(
            targetValue = if (id.index in selected) STRING_WIDTH * 1.5f else STRING_WIDTH,
            label = "harpStringWidth${id.index}"
        ).value
    }

    val painter = rememberVectorPainter(
        defaultWidth = 150.dp,
        defaultHeight = 200.dp,
        viewportWidth = VIEWPORT_WIDTH,
        viewportHeight = VIEWPORT_HEIGHT,
        autoMirror = false
    ) { _, _ ->
        Group(translationX = OFFSET_X, translationY = OFFSET_Y) {
            Path(pathData = framePath, fill = SolidColor(colors.secondary))
            Group(clipPathData = stringsMask) {
                stringPaths.forEachIndexed { index, path ->
                    Path(
                        pathData = path,
                        stroke = SolidColor(stringColors[index]),
                        strokeLineWidth = stringWidths[index],
                        strokeLineCap = StrokeCap.Round
                    )
                }
            }
        }
    }

    Image(
        painter = painter,
        contentDescription = null,
        modifier = modifier.aspectRatio(3f / 4f, matchHeightConstraintsFirst = true)
    )
}
