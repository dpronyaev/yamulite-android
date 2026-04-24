package dev.pdv.yamulite.ui.main.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pdv.yamulite.data.music.SearchType
import dev.pdv.yamulite.ui.main.components.AlbumRow
import dev.pdv.yamulite.ui.main.components.ArtistRow
import dev.pdv.yamulite.ui.main.components.TrackRow

@Composable
fun SearchScreen(vm: SearchViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val likedIds by vm.likedIds.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.query,
            onValueChange = vm::onQueryChange,
            placeholder = { Text("Поиск") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = ImeAction.Search,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )

        val tabs = listOf(SearchType.Tracks, SearchType.Artists, SearchType.Albums)
        val labels = mapOf(
            SearchType.Tracks to "Треки",
            SearchType.Artists to "Исполнители",
            SearchType.Albums to "Альбомы",
        )
        TabRow(selectedTabIndex = tabs.indexOf(state.type)) {
            tabs.forEach { t ->
                Tab(
                    selected = state.type == t,
                    onClick = { vm.onTypeChange(t) },
                    text = { Text(labels.getValue(t)) },
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center).padding(top = 24.dp),
                )
                state.error != null -> Text(
                    "Ошибка: ${state.error}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                )
                state.query.isBlank() -> Text(
                    "Начните вводить запрос",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                )
                else -> Results(state, likedIds, vm::toggleLike, vm::play)
            }
        }
    }
}

@Composable
private fun Results(
    state: SearchUiState,
    likedIds: Set<String>,
    onToggleLike: (String) -> Unit,
    onPlay: (List<dev.pdv.yamulite.data.music.dto.TrackDto>, Int) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        when (state.type) {
            SearchType.Tracks -> {
                itemsIndexed(state.results.tracks, key = { _, t -> t.id }) { idx, track ->
                    TrackRow(
                        track = track,
                        isLiked = track.id in likedIds,
                        onClick = { onPlay(state.results.tracks, idx) },
                        onLikeToggle = { onToggleLike(track.id) },
                    )
                }
                if (state.results.tracks.isEmpty()) item { EmptyHint() }
            }
            SearchType.Artists -> {
                items(state.results.artists, key = { it.id }) { artist ->
                    ArtistRow(artist)
                }
                if (state.results.artists.isEmpty()) item { EmptyHint() }
            }
            SearchType.Albums -> {
                items(state.results.albums, key = { it.id }) { album ->
                    AlbumRow(album)
                }
                if (state.results.albums.isEmpty()) item { EmptyHint() }
            }
        }
    }
}

@Composable
private fun EmptyHint() {
    Text(
        "Ничего не найдено",
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
