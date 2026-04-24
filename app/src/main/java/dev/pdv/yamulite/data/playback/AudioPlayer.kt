package dev.pdv.yamulite.data.playback

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.pdv.yamulite.data.music.StreamUrlResolver
import dev.pdv.yamulite.data.music.dto.TrackDto
import dev.pdv.yamulite.data.settings.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class PlaybackUi(
    val track: TrackDto? = null,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
)

@Singleton
class AudioPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val resolver: StreamUrlResolver,
    private val settings: SettingsStore,
    private val downloads: DownloadManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var player: ExoPlayer? = null
    private var queue: List<TrackDto> = emptyList()
    private var index: Int = -1
    private var resolveJob: Job? = null

    private val _state = MutableStateFlow(PlaybackUi())
    val state: StateFlow<PlaybackUi> = _state.asStateFlow()

    private fun ensurePlayer(): ExoPlayer {
        player?.let { return it }
        val p = ExoPlayer.Builder(context).build()
        p.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.value = _state.value.copy(isPlaying = isPlaying)
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    if (hasNext()) next() else _state.value = _state.value.copy(isPlaying = false)
                }
            }
            override fun onPlayerError(error: PlaybackException) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    isPlaying = false,
                    error = error.errorCodeName,
                )
            }
        })
        player = p
        return p
    }

    fun play(tracks: List<TrackDto>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        queue = tracks
        index = startIndex.coerceIn(0, tracks.size - 1)
        playCurrent()
    }

    fun togglePlayPause() {
        val p = player ?: return
        if (p.isPlaying) p.pause() else p.play()
    }

    fun next() {
        if (!hasNext()) return
        index++
        playCurrent()
    }

    fun previous() {
        if (!hasPrevious()) return
        index--
        playCurrent()
    }

    fun stop() {
        resolveJob?.cancel()
        player?.stop()
        queue = emptyList()
        index = -1
        _state.value = PlaybackUi()
    }

    private fun hasNext() = index in 0 until (queue.size - 1)
    private fun hasPrevious() = index > 0

    private fun playCurrent() {
        val track = queue.getOrNull(index) ?: return
        resolveJob?.cancel()
        _state.value = PlaybackUi(
            track = track,
            isLoading = true,
            hasNext = hasNext(),
            hasPrevious = hasPrevious(),
        )
        resolveJob = scope.launch {
            val url = downloads.localPath(track.id)?.let { "file://$it" }
                ?: runCatching {
                    val q = settings.currentQuality()
                    resolver.resolve(track.id, q.bitrate)
                }.getOrElse {
                    _state.value = _state.value.copy(isLoading = false, error = it.message ?: "resolve failed")
                    return@launch
                }
            if (url == null) {
                _state.value = _state.value.copy(isLoading = false, error = "Нет ссылки на трек")
                return@launch
            }
            withContext(Dispatchers.Main) {
                val p = ensurePlayer()
                p.setMediaItem(MediaItem.fromUri(url))
                p.prepare()
                p.playWhenReady = true
                _state.value = _state.value.copy(isLoading = false, error = null)
            }
        }
    }
}
