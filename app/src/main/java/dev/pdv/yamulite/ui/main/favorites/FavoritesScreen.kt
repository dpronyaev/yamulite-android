package dev.pdv.yamulite.ui.main.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pdv.yamulite.ui.main.components.TrackRow

@Composable
fun FavoritesScreen(vm: FavoritesViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val likedIds by vm.likedIds.collectAsStateWithLifecycle()
    val downloadStates by vm.downloadStates.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.loading && state.tracks.isEmpty() ->
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            state.error != null -> Column(
                modifier = Modifier.align(Alignment.Center).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Ошибка: ${state.error}", color = MaterialTheme.colorScheme.error)
                Button(onClick = vm::refresh) { Text("Повторить") }
            }
            state.tracks.isEmpty() -> Text(
                "В избранном пусто",
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
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
