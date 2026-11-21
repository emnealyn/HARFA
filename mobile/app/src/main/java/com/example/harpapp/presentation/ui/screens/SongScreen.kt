package com.example.harpapp.presentation.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.harpapp.R
import com.example.harpapp.domain.model.AudioPlaybackProgress
import com.example.harpapp.domain.model.CatalogIssue
import com.example.harpapp.domain.model.Difficulty
import com.example.harpapp.domain.model.LyricNote
import com.example.harpapp.domain.model.Song
import com.example.harpapp.domain.model.TuningAnalysis
import com.example.harpapp.presentation.ui.components.home.getCoverResourceId
import com.example.harpapp.presentation.ui.components.song.CompletedSongDialog
import com.example.harpapp.presentation.ui.components.song.LessonIssuesDialog
import com.example.harpapp.presentation.ui.components.song.LyricsDisplay
import com.example.harpapp.presentation.ui.components.song.StepTuningHintBanner
import com.example.harpapp.presentation.ui.components.song.TuningMismatchDialog
import com.example.harpapp.presentation.ui.components.song.TuningWarningBanner
import com.example.harpapp.presentation.ui.theme.HARPAppTheme
import com.example.harpapp.presentation.viewmodel.SongUiState
import com.example.harpapp.presentation.viewmodel.SongViewModel
import com.example.harpapp.presentation.viewmodel.StepTuningHint
import kotlin.math.max
import kotlin.math.roundToInt

private val PLAYBACK_WAVEFORM_HEIGHTS = floatArrayOf(
    0.28f, 0.52f, 0.38f, 0.72f, 0.46f, 0.86f, 0.62f, 0.34f,
    0.58f, 0.78f, 0.42f, 0.68f, 0.32f, 0.54f, 0.9f, 0.5f,
    0.74f, 0.4f, 0.64f, 0.82f, 0.36f, 0.56f, 0.76f, 0.44f
)

@Composable
fun SongScreen(
    songId: String,
    viewModel: SongViewModel,
    onNavigateToHarp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState
    val isPlaying by viewModel.isPlaying
    val isPreparingAudio by viewModel.isPreparingAudio
    val audioPlaybackError by viewModel.audioPlaybackError
    val audioPlaybackProgress by viewModel.audioPlaybackProgress
    val isLearningMode by viewModel.isLearningMode
    val currentNoteIndex by viewModel.currentNoteIndex
    val learningComplete by viewModel.learningComplete
    val lessonIssues by viewModel.lessonIssues
    val isLessonPaused by viewModel.isLessonPaused
    val canToggleLessonPause by viewModel.canToggleLessonPause
    val isPausedByConnection by viewModel.isPausedByConnection
    val tuningWarning by viewModel.tuningWarning
    val tuningStartWarning by viewModel.tuningStartWarning
    val stepHint by viewModel.currentStepHint
    val songTuning by viewModel.songTuning

    when (val state = uiState) {
        SongUiState.Loading -> SongLoadingScreen(modifier)
        SongUiState.NotFound -> SongNotFoundScreen(modifier)
        is SongUiState.Error -> SongErrorScreen(
            onRetry = { viewModel.loadSong(songId) },
            modifier = modifier
        )
        is SongUiState.Ready -> SongContent(
            song = state.song,
            isPlaying = isPlaying,
            isPreparingAudio = isPreparingAudio,
            audioPlaybackError = audioPlaybackError,
            audioPlaybackProgress = audioPlaybackProgress,
            onPlayPauseClick = { viewModel.togglePlayPause() },
            onSeek = { positionMs -> viewModel.seekTo(positionMs) },
            isLearningMode = isLearningMode,
            onLearningModeClick = { viewModel.toggleLearningMode() },
            currentNoteIndex = if (isLearningMode) currentNoteIndex else -1,
            learningComplete = learningComplete,
            onDismissDialog = { viewModel.dismissLearningCompleteDialog() },
            lessonIssues = lessonIssues,
            onDismissLessonIssues = { viewModel.dismissLessonIssues() },
            isLessonPaused = isLessonPaused,
            canToggleLessonPause = canToggleLessonPause,
            onLessonPauseClick = { viewModel.toggleLessonPause() },
            onPlayAgainClick = { viewModel.restartLearning() },
            isPausedByConnection = isPausedByConnection,
            songKey = songTuning?.name,
            tuningWarning = tuningWarning,
            stepHint = stepHint,
            tuningStartWarning = tuningStartWarning,
            onNavigateToHarp = onNavigateToHarp,
            onStartDespiteTuning = { viewModel.confirmStartDespiteTuning() },
            onCancelTuningStart = { viewModel.cancelTuningStart() },
            onStartDialogGoToHarp = {
                viewModel.cancelTuningStart()
                onNavigateToHarp()
            },
            modifier = modifier
        )
    }
}

@Composable
private fun ConnectionLostBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.BluetoothDisabled,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.lesson_paused_no_connection),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SongKeyText(key: String) {
    Text(
        text = stringResource(R.string.song_key, key),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun TuningBanners(
    tuningWarning: TuningAnalysis?,
    stepHint: StepTuningHint?,
    keyName: String?,
    onNavigateToHarp: () -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        stepHint != null -> StepTuningHintBanner(
            pitch = stepHint.pitch,
            keyName = keyName,
            analysis = stepHint.analysis,
            onNavigateToHarp = onNavigateToHarp,
            modifier = modifier
        )
        tuningWarning != null -> TuningWarningBanner(
            analysis = tuningWarning,
            keyName = keyName,
            onNavigateToHarp = onNavigateToHarp,
            modifier = modifier
        )
    }
}

/** Przycisk wstrzymania/wznowienia lekcji, widoczny tylko w trakcie nauki. */
@Composable
private fun LessonPauseButton(
    isPaused: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Icon(
            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isPaused) stringResource(R.string.resume_learning)
            else stringResource(R.string.pause_learning)
        )
    }
}

@Composable
private fun ReferenceTrackButton(
    isPlaying: Boolean,
    isPreparingAudio: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonContentDescription = when {
        isPreparingAudio -> stringResource(R.string.reference_track_preparing)
        isPlaying -> stringResource(R.string.reference_track_pause)
        else -> stringResource(R.string.reference_track_play)
    }

    OutlinedIconButton(
        onClick = onClick,
        enabled = !isPreparingAudio,
        modifier = modifier.semantics {
            contentDescription = buttonContentDescription
        },
        shape = CircleShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        colors = IconButtonDefaults.outlinedIconButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
        )
    ) {
        if (isPreparingAudio) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ReferenceTrackControls(
    progress: AudioPlaybackProgress,
    fallbackDurationText: String,
    isPlaying: Boolean,
    isPreparingAudio: Boolean,
    audioPlaybackError: Throwable?,
    onPlayPauseClick: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalTimeText = if (progress.durationMs > 0L) {
        formatPlaybackTime(progress.durationMs)
    } else {
        fallbackDurationText
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                PlaybackWaveform(
                    progress = progress,
                    enabled = !isPreparingAudio && progress.durationMs > 0L,
                    onSeek = { fraction ->
                        val positionMs =
                            (progress.durationMs.toDouble() * fraction).toLong()
                        onSeek(positionMs)
                    }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatPlaybackTime(progress.positionMs),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = totalTimeText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            ReferenceTrackButton(
                isPlaying = isPlaying,
                isPreparingAudio = isPreparingAudio,
                onClick = onPlayPauseClick,
                modifier = Modifier.size(48.dp)
            )
        }

        when {
            isPreparingAudio -> Text(
                text = stringResource(R.string.reference_track_preparing),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            audioPlaybackError != null -> Text(
                text = stringResource(R.string.reference_track_error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun PlaybackWaveform(
    progress: AudioPlaybackProgress,
    enabled: Boolean,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val playedFraction = if (progress.durationMs > 0L) progress.fraction else 0f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Canvas(
            modifier = Modifier.matchParentSize()
        ) {
            val gap = 3.dp.toPx()
            val sliderThumbHalfWidth = 2.dp.toPx()
            val trackStart = sliderThumbHalfWidth
            val trackWidth = (size.width - sliderThumbHalfWidth * 2f).coerceAtLeast(0f)
            val originalBarCount = PLAYBACK_WAVEFORM_HEIGHTS.size
            val originalBarWidth = max(
                1.dp.toPx(),
                (trackWidth - gap * (originalBarCount - 1)) / originalBarCount
            )
            val targetBarWidth = max(1.dp.toPx(), originalBarWidth / 2f)
            val barCount = max(
                1,
                ((trackWidth + gap) / (targetBarWidth + gap)).toInt()
            )
            val barWidth = max(
                1.dp.toPx(),
                (trackWidth - gap * (barCount - 1)) / barCount
            )
            val centerY = size.height / 2f
            val maxHeight = size.height * 0.78f

            fun drawBars(color: Color) {
                repeat(barCount) { index ->
                    val shapeIndex = if (barCount == 1) {
                        0
                    } else {
                        (
                            index.toFloat() / (barCount - 1) *
                                (PLAYBACK_WAVEFORM_HEIGHTS.lastIndex)
                            ).roundToInt()
                    }
                    val x = trackStart + barWidth / 2f + index * (barWidth + gap)
                    val height = max(
                        4.dp.toPx(),
                        maxHeight * PLAYBACK_WAVEFORM_HEIGHTS[shapeIndex]
                    )
                    drawLine(
                        color = color,
                        start = Offset(x, centerY - height / 2f),
                        end = Offset(x, centerY + height / 2f),
                        strokeWidth = barWidth,
                        cap = StrokeCap.Round
                    )
                }
            }

            drawBars(colors.onSurfaceVariant.copy(alpha = 0.32f))
            clipRect(
                right = trackStart + trackWidth * playedFraction
            ) {
                drawBars(colors.primary)
            }
        }

        Slider(
            value = progress.fraction,
            onValueChange = onSeek,
            enabled = enabled,
            modifier = Modifier.fillMaxSize(),
            colors = SliderDefaults.colors(
                thumbColor = colors.primary,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent,
                disabledThumbColor = if (progress.durationMs > 0L) {
                    colors.primary.copy(alpha = 0.5f)
                } else {
                    Color.Transparent
                },
                disabledActiveTrackColor = Color.Transparent,
                disabledInactiveTrackColor = Color.Transparent
            )
        )
    }
}

private fun formatPlaybackTime(timeMs: Long): String {
    val totalSeconds = timeMs.coerceAtLeast(0L) / 1_000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

@Composable
fun SongContent(
    song: Song,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    isLearningMode: Boolean,
    onLearningModeClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPreparingAudio: Boolean = false,
    audioPlaybackError: Throwable? = null,
    audioPlaybackProgress: AudioPlaybackProgress = AudioPlaybackProgress.EMPTY,
    onSeek: (Long) -> Unit = {},
    currentNoteIndex: Int = -1,
    learningComplete: Boolean = false,
    onDismissDialog: () -> Unit = {},
    lessonIssues: List<CatalogIssue> = emptyList(),
    onDismissLessonIssues: () -> Unit = {},
    isLessonPaused: Boolean = false,
    canToggleLessonPause: Boolean = false,
    onLessonPauseClick: () -> Unit = {},
    onPlayAgainClick: () -> Unit = {},
    isPausedByConnection: Boolean = false,
    songKey: String? = null,
    tuningWarning: TuningAnalysis? = null,
    stepHint: StepTuningHint? = null,
    tuningStartWarning: TuningAnalysis? = null,
    onNavigateToHarp: () -> Unit = {},
    onStartDespiteTuning: () -> Unit = {},
    onCancelTuningStart: () -> Unit = {},
    onStartDialogGoToHarp: () -> Unit = {},
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val showLessonPause = isLearningMode && !learningComplete

    val imageResId = song.getCoverResourceId()

    if (isLandscape) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier
                        .size(140.dp)
                        .aspectRatio(1f),
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 4.dp
                ) {
                    Image(
                        painter = painterResource(id = imageResId),
                        contentDescription = "Album Cover",
                        contentScale = ContentScale.Crop
                    )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ReferenceTrackControls(
                        progress = audioPlaybackProgress,
                        fallbackDurationText = song.duration,
                        isPlaying = isPlaying,
                        isPreparingAudio = isPreparingAudio,
                        audioPlaybackError = audioPlaybackError,
                        onPlayPauseClick = onPlayPauseClick,
                        onSeek = onSeek
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onLearningModeClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                text = if (isLearningMode) stringResource(R.string.stop_learning)
                                else stringResource(R.string.start_learning),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    if (showLessonPause) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LessonPauseButton(
                            isPaused = isLessonPaused,
                            enabled = canToggleLessonPause,
                            onClick = onLessonPauseClick
                        )
                    }

                    if (isPausedByConnection) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ConnectionLostBanner()
                    }

                    TuningBanners(
                        tuningWarning = tuningWarning,
                        stepHint = stepHint,
                        keyName = songKey,
                        onNavigateToHarp = onNavigateToHarp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1.2f)
                ) {
                    Text(
                        text = stringResource(R.string.lyrics_notes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    songKey?.let { SongKeyText(it) }

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        )
                    ) {
                        Box(modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                        ) {
                            LyricsDisplay(
                                lyrics = song.lyricsWithNotes,
                                currentNoteIndex = currentNoteIndex,
                                learningComplete = learningComplete
                            )
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    modifier = Modifier
                        .size(220.dp)
                        .aspectRatio(1f),
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 8.dp
                ) {
                    Image(
                        painter = painterResource(id = imageResId),
                        contentDescription = "Album Cover",
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = song.title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                ReferenceTrackControls(
                    progress = audioPlaybackProgress,
                    fallbackDurationText = song.duration,
                    isPlaying = isPlaying,
                    isPreparingAudio = isPreparingAudio,
                    audioPlaybackError = audioPlaybackError,
                    onPlayPauseClick = onPlayPauseClick,
                    onSeek = onSeek
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onLearningModeClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text(
                            text = if (isLearningMode) stringResource(R.string.stop_learning)
                            else stringResource(R.string.start_learning),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (showLessonPause) {
                    LessonPauseButton(
                        isPaused = isLessonPaused,
                        enabled = canToggleLessonPause,
                        onClick = onLessonPauseClick,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                if (isPausedByConnection) {
                    ConnectionLostBanner(modifier = Modifier.padding(bottom = 16.dp))
                }

                TuningBanners(
                    tuningWarning = tuningWarning,
                    stepHint = stepHint,
                    keyName = songKey,
                    onNavigateToHarp = onNavigateToHarp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = stringResource(R.string.lyrics_notes),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    songKey?.let { SongKeyText(it) }

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            LyricsDisplay(
                                lyrics = song.lyricsWithNotes,
                                currentNoteIndex = currentNoteIndex,
                                learningComplete = learningComplete
                            )
                        }
                    }
                }
            }
        }

    if (learningComplete) {
        CompletedSongDialog(onDismiss = onDismissDialog, onPlayAgain = onPlayAgainClick)
    }

    if (lessonIssues.isNotEmpty()) {
        LessonIssuesDialog(issues = lessonIssues, onDismiss = onDismissLessonIssues)
    }

    tuningStartWarning?.let { analysis ->
        TuningMismatchDialog(
            analysis = analysis,
            keyName = songKey,
            onGoToHarp = onStartDialogGoToHarp,
            onStartAnyway = onStartDespiteTuning,
            onDismiss = onCancelTuningStart
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SongScreenPreview() {
    HARPAppTheme {
        val mockSong = Song(
            id = "1",
            title = "Wildest Dreams",
            artist = "Taylor Swift",
            difficulty = Difficulty.MEDIUM,
            duration = "2:55",
            coverImage = "cover_placeholder",
            lyricsWithNotes = listOf(
                LyricNote("He", "D4"),
                LyricNote("said", "D4"),
                LyricNote("Let's", "A4"),
                LyricNote("get", "C4"),
                LyricNote("out", "D4")
            )
        )
        Surface(color = MaterialTheme.colorScheme.background) {
            SongContent(
                song = mockSong,
                isPlaying = false,
                onPlayPauseClick = {},
                isLearningMode = true,
                onLearningModeClick = {},
                currentNoteIndex = 2,
                learningComplete = false
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=1280dp,height=800dp,orientation=landscape")
@Composable
fun SongScreenLandscapePreview() {
    HARPAppTheme {
        val mockSong = Song(
            id = "1",
            title = "Wildest Dreams",
            artist = "Taylor Swift",
            difficulty = Difficulty.MEDIUM,
            duration = "2:55",
            coverImage = "cover_placeholder",
            lyricsWithNotes = listOf(
                LyricNote("He", "D4"),
                LyricNote("said", "D4"),
                LyricNote("Let's", "A4"),
                LyricNote("get", "C4"),
                LyricNote("out", "D4")
            )
        )
        Surface(color = MaterialTheme.colorScheme.background) {
            SongContent(
                song = mockSong,
                isPlaying = false,
                onPlayPauseClick = {},
                isLearningMode = true,
                onLearningModeClick = {},
                currentNoteIndex = 2,
                learningComplete = false
            )
        }
    }
}
