package dev.pdv.yamulite.ui.main.nowplaying

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pdv.yamulite.data.music.dto.ArtistShortDto
import dev.pdv.yamulite.data.music.dto.TrackDto
import dev.pdv.yamulite.data.playback.PlaybackUi
import dev.pdv.yamulite.ui.main.components.CoverImage
import dev.pdv.yamulite.ui.main.components.FavoriteToggleButton

@Composable
fun NowPlayingScreen(
    onArtistClick: (Long) -> Unit = {},
    onAlbumClick: (Long) -> Unit = {},
    vm: NowPlayingViewModel = hiltViewModel(),
) {
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
                    onArtistClick = onArtistClick,
                    onAlbumClick = onAlbumClick,
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
                    onArtistClick = onArtistClick,
                    onAlbumClick = onAlbumClick,
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
    onArtistClick: (Long) -> Unit,
    onAlbumClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = modifier,
    ) {
        val hasTitle = !track.title.isNullOrBlank()
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            when {
                hasTitle -> {
                    Text(
                        text = track.title!!,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (track.artists.isNotEmpty()) {
                        ArtistLinks(
                            artists = track.artists,
                            style = MaterialTheme.typography.bodyLarge,
                            onArtistClick = onArtistClick,
                        )
                    }
                }
                track.artists.isNotEmpty() -> ArtistLinks(
                    artists = track.artists,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    onArtistClick = onArtistClick,
                )
                else -> Text(
                    text = "(без названия)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
            }
            track.albums.firstOrNull()?.let { album ->
                val albumTitle = album.title
                if (!albumTitle.isNullOrBlank()) {
                    val clickable = album.id != 0L
                    Text(
                        text = albumTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .let { if (clickable) it.clickable { onAlbumClick(album.id) } else it },
                    )
                }
            }
        }
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
            FavoriteToggleButton(
                isLiked = track.id in likedIds,
                onToggle = vm::toggleLike,
                modifier = Modifier.size(64.dp),
                iconSize = 40.dp,
            )
        }
    }
}

@Composable
private fun ArtistLinks(
    artists: List<ArtistShortDto>,
    style: TextStyle,
    onArtistClick: (Long) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth(),
    ) {
        artists.forEachIndexed { index, artist ->
            if (index > 0) {
                Text(", ", style = style, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val clickable = artist.id != 0L
            Text(
                text = artist.name,
                style = style,
                color = if (clickable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = if (clickable) Modifier.clickable { onArtistClick(artist.id) } else Modifier,
            )
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
