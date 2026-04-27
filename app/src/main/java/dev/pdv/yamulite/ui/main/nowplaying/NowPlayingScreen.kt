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
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
        SeekBar(state = state, onSeek = vm::seekTo)
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

@Composable
private fun SeekBar(state: PlaybackUi, onSeek: (Long) -> Unit) {
    val duration = state.durationMs
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableStateOf(0f) }
    val sliderValue = if (dragging) dragValue
    else if (duration > 0) state.positionMs.coerceIn(0L, duration).toFloat()
    else 0f
    val maxValue = if (duration > 0) duration.toFloat() else 1f
    val displayMs = if (dragging) dragValue.toLong() else state.positionMs

    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = sliderValue.coerceIn(0f, maxValue),
            valueRange = 0f..maxValue,
            enabled = duration > 0,
            onValueChange = {
                dragging = true
                dragValue = it
            },
            onValueChangeFinished = {
                if (dragging) {
                    onSeek(dragValue.toLong())
                    dragging = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        ) {
            Text(
                text = formatTime(displayMs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatTime(duration),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
