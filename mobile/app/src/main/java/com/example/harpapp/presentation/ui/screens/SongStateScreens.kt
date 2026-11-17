package com.example.harpapp.presentation.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.harpapp.R

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
    SongStateMessage(
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
    SongStateMessage(
        title = stringResource(R.string.song_loading_error_title),
        message = stringResource(R.string.song_loading_error_message),
        onAction = onRetry,
        actionLabel = stringResource(R.string.retry),
        modifier = modifier
    )
}

@Composable
private fun SongStateMessage(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    onAction: (() -> Unit)? = null,
    actionLabel: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp)
        )
        if (onAction != null && actionLabel != null) {
            Button(
                onClick = onAction,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Text(text = actionLabel)
            }
        }
    }
}
