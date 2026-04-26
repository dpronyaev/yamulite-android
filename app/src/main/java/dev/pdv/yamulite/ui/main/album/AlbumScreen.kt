package dev.pdv.yamulite.ui.main.album

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pdv.yamulite.ui.main.components.CoverImage
import dev.pdv.yamulite.ui.main.components.TrackRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumScreen(
    onBack: () -> Unit,
    vm: AlbumViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val likedIds by vm.likedIds.collectAsStateWithLifecycle()
    val downloadStates by vm.downloadStates.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.title.ifBlank { "Альбом" }, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.error != null -> Column(
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Ошибка: ${state.error}", color = MaterialTheme.colorScheme.error)
                    Button(onClick = vm::refresh) { Text("Повторить") }
                }
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item { Header(state) }
                    if (state.tracks.isEmpty()) {
                        item {
                            Text(
                                "В альбоме нет треков",
                                modifier = Modifier.padding(24.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        itemsIndexed(state.tracks, key = { _, t -> t.id }) { idx, track ->
                            TrackRow(
                                track = track,
                                isLiked = track.id in likedIds,
                                download = downloadStates[track.id],
                                onClick = { vm.play(state.tracks, idx) },
                                onLikeToggle = { vm.toggleLike(track.id) },
                                onDownloadClick = { vm.onDownloadClick(track.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(state: AlbumUiState) {
    Row(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverImage(coverUri = state.coverUri, side = 120.dp, pixelSize = 320)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(state.title, style = MaterialTheme.typography.titleLarge)
            if (state.artistsLine.isNotBlank()) {
                Text(state.artistsLine, style = MaterialTheme.typography.bodyMedium)
            }
            state.year?.let {
                Text(it.toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
