package com.example.harpapp.presentation.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.harpapp.R
import com.example.harpapp.presentation.ui.theme.DisplayFontFamily
import com.example.harpapp.presentation.ui.theme.HARPAppTheme
import com.example.harpapp.presentation.ui.theme.extendedColors

@Composable
fun SongLoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun SongNotFoundScreen(modifier: Modifier = Modifier) {
    SongStatePlaceholder(
        icon = Icons.Default.SearchOff,
        title = stringResource(R.string.song_not_found_title),
        message = stringResource(R.string.song_not_found_message),
        modifier = modifier
    )
}

@Composable
fun SongErrorScreen(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    SongStatePlaceholder(
        icon = Icons.Default.ErrorOutline,
        title = stringResource(R.string.song_loading_error_title),
        message = stringResource(R.string.song_loading_error_message),
        accent = MaterialTheme.colorScheme.error,
        actionLabel = stringResource(R.string.retry),
        actionIcon = Icons.Default.Refresh,
        onAction = onRetry,
        modifier = modifier
    )
}

@Composable
internal fun SongStatePlaceholder(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.extendedColors.navSelected,
    actionLabel: String? = null,
    actionIcon: ImageVector? = null,
    onAction: (() -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = accent.copy(alpha = 0.12f),
            modifier = Modifier.size(96.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(44.dp)
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = DisplayFontFamily,
            fontWeight = FontWeight.Bold,
            color = colors.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 24.dp)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 8.dp)
                .widthIn(max = 320.dp)
        )
        if (onAction != null && actionLabel != null) {
            Button(
                onClick = onAction,
                contentPadding = if (actionIcon != null) {
                    ButtonDefaults.ButtonWithIconContentPadding
                } else {
                    ButtonDefaults.ContentPadding
                },
                modifier = Modifier.padding(top = 28.dp)
            ) {
                if (actionIcon != null) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                }
                Text(text = actionLabel)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SongNotFoundScreenPreview() {
    HARPAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            SongNotFoundScreen()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SongErrorScreenPreview() {
    HARPAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            SongErrorScreen(onRetry = {})
        }
    }
}
