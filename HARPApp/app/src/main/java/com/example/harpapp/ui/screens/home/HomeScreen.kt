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
import androidx.compose.ui.unit.dp
import com.example.harpapp.ui.components.home.SongList
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.harpapp.model.Difficulty
import com.example.harpapp.model.Song
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.ui.res.stringResource
import com.example.harpapp.R

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToSong: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val songs by viewModel.filteredSongs.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedDifficulties by viewModel.selectedDifficulties.collectAsState()
    val showFavoritesOnly by viewModel.showFavoritesOnly.collectAsState()

    HomeContent(
        songs = songs,
        searchQuery = searchQuery,
        selectedDifficulties = selectedDifficulties,
        showFavoritesOnly = showFavoritesOnly,
        onDifficultyToggle = { difficulty -> viewModel.toggleDifficultyFilter(difficulty) },
        onToggleFavoritesFilter = { viewModel.toggleFavoritesFilter() },
        onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
        onSongClick = onNavigateToSong,
        onFavoriteToggle = { song -> viewModel.toggleFavorite(song.id, song.isFavorite) },
        modifier = modifier
    )
}

@Composable
fun HomeContent(
    songs: List<Song>,
    searchQuery: String,
    selectedDifficulties: Set<Difficulty>,
    showFavoritesOnly: Boolean,
    onDifficultyToggle: (Difficulty) -> Unit,
    onToggleFavoritesFilter: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onFavoriteToggle: (Song) -> Unit,
    onSongClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.what_will_you_play_today),
            fontSize = 25.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_for_song_or_artist),
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                },
                trailingIcon = {
                    Row(
                        modifier = Modifier.padding(end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        IconButton(onClick = onToggleFavoritesFilter) {
                            Icon(
                                imageVector = if (showFavoritesOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Filter Favorites",
                                tint = if (showFavoritesOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        }

                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filter",
                                tint = if (selectedDifficulties.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                )
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                Difficulty.values().forEach { difficulty ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = selectedDifficulties.contains(difficulty),
                                    onCheckedChange = { onDifficultyToggle(difficulty) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Text(
                                    text = difficulty.name,
                                    modifier = Modifier.padding(start = 8.dp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        onClick = { onDifficultyToggle(difficulty) }
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.your_library),
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )

        SongList(
            songs = songs,
            onFavoriteClick = onFavoriteToggle,
            onSongClick = { song -> onSongClick(song.id) },
            modifier = Modifier.weight(1f)
        )
    }
}


