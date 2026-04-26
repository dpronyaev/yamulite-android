package dev.pdv.yamulite.ui.main.nowplaying

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pdv.yamulite.data.music.dto.TrackDto
import dev.pdv.yamulite.data.playback.PlaybackUi
import dev.pdv.yamulite.ui.main.components.CoverImage
import dev.pdv.yamulite.ui.main.components.displayLine

@Composable
fun NowPlayingScreen(vm: NowPlayingViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val likedIds by vm.likedIds.collectAsStateWithLifecycle()

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        val track = state.track
        if (track == null) {
            Text(
                "Сейчас ничего не играет",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@BoxWithConstraints
        }
        val landscape = maxWidth > maxHeight
        val coverSide = if (landscape) min(maxHeight, 280.dp) else 280.dp
        if (landscape) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CoverImage(
                    coverUri = track.coverUri ?: track.albums.firstOrNull()?.coverUri,
                    side = coverSide,
                    pixelSize = 600,
                )
                TrackDetails(
                    track = track,
                    state = state,
                    likedIds = likedIds,
                    vm = vm,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                CoverImage(
                    coverUri = track.coverUri ?: track.albums.firstOrNull()?.coverUri,
                    side = coverSide,
                    pixelSize = 600,
                )
                TrackDetails(
                    track = track,
                    state = state,
                    likedIds = likedIds,
                    vm = vm,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun TrackDetails(
    track: TrackDto,
    state: PlaybackUi,
    likedIds: Set<String>,
    vm: NowPlayingViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = modifier,
    ) {
        Text(
            text = track.displayLine(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        state.error?.let {
            Text("Ошибка: $it", color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            IconButton(
                onClick = vm::previous,
                enabled = state.hasPrevious,
                modifier = Modifier.size(64.dp),
            ) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "Предыдущий", modifier = Modifier.size(40.dp))
            }
            IconButton(onClick = vm::togglePlayPause, modifier = Modifier.size(72.dp)) {
                when {
                    state.isLoading -> CircularProgressIndicator(modifier = Modifier.size(48.dp))
                    state.isPlaying -> Icon(Icons.Filled.Pause, contentDescription = "Пауза", modifier = Modifier.size(48.dp))
                    else -> Icon(Icons.Filled.PlayArrow, contentDescription = "Играть", modifier = Modifier.size(48.dp))
                }
            }
            IconButton(
                onClick = vm::next,
                enabled = state.hasNext,
                modifier = Modifier.size(64.dp),
            ) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Следующий", modifier = Modifier.size(40.dp))
            }
            IconButton(onClick = vm::toggleLike, modifier = Modifier.size(64.dp)) {
                val liked = track.id in likedIds
                Icon(
                    imageVector = if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (liked) "Убрать из избранного" else "В избранное",
                    tint = if (liked) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(40.dp),
                )
            }
        }
    }
}
