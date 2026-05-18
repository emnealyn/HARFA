package com.example.harpapp.ui.screens.home

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.harpapp.viewmodel.HomeViewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.harpapp.ui.components.SongList
import com.example.harpapp.ui.theme.HARPAppTheme
import android.annotation.SuppressLint
import com.example.harpapp.ui.theme.HARPAppTheme

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val songs by viewModel.filteredSongs.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        SongList(
            songs = songs,
            onSongPreviewClick = { song ->
                viewModel.playSongPreview(song.id)
            }
        )
    }
}


@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    HARPAppTheme {
        HomeScreen(viewModel = HomeViewModel())
    }
}