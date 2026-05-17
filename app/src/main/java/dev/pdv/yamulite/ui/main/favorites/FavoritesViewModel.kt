package dev.pdv.yamulite.ui.main.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pdv.yamulite.data.music.MusicRepository
import dev.pdv.yamulite.data.music.dto.LikedTrackRefDto
import dev.pdv.yamulite.data.music.dto.TrackDto
import dev.pdv.yamulite.data.playback.AudioPlayer
import dev.pdv.yamulite.data.playback.DownloadManager
import dev.pdv.yamulite.data.playback.DownloadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoritesUiState(
    val loading: Boolean = false,
    val tracks: List<TrackDto> = emptyList(),
    val error: String? = null,
    val likeError: String? = null,
    val hasMore: Boolean = false,
    val loadingMore: Boolean = false,
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repo: MusicRepository,
    private val player: AudioPlayer,
    private val downloads: DownloadManager,
) : ViewModel() {

    val downloadStates = downloads.downloads
    val likedIds = repo.likedIds

    private val _state = MutableStateFlow(FavoritesUiState())
    val state: StateFlow<FavoritesUiState> = _state.asStateFlow()

    private var allRefs: List<LikedTrackRefDto> = emptyList()
    private var loadedPage = -1

    init { refresh() }

    fun play(tracks: List<TrackDto>, startIndex: Int) = player.play(tracks, startIndex)

    fun onDownloadClick(trackId: String) {
        if (downloads.downloads.value[trackId]?.state == DownloadState.Done) {
            downloads.delete(trackId)
        } else {
            downloads.download(trackId)
        }
    }

    fun refresh() = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        runCatching {
            allRefs = repo.likedTrackRefs()
            loadedPage = -1
            if (allRefs.isEmpty()) {
                FavoritesUiState()
            } else {
                val first = repo.favoritesPage(allRefs, 0)
                loadedPage = 0
                FavoritesUiState(
                    tracks = first,
                    hasMore = allRefs.size > MusicRepository.FAVORITES_PAGE_SIZE,
                )
            }
        }
            .onSuccess { _state.value = it }
            .onFailure { _state.value = FavoritesUiState(error = it.message ?: "Ошибка загрузки") }
    }

    fun loadMore() {
        if (_state.value.loadingMore || !_state.value.hasMore) return
        viewModelScope.launch {
            _state.value = _state.value.copy(loadingMore = true)
            val nextPage = loadedPage + 1
            runCatching { repo.favoritesPage(allRefs, nextPage) }
                .onSuccess { newTracks ->
                    loadedPage = nextPage
                    _state.value = _state.value.copy(
                        tracks = _state.value.tracks + newTracks,
                        loadingMore = false,
                        hasMore = (nextPage + 1) * MusicRepository.FAVORITES_PAGE_SIZE < allRefs.size,
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(loadingMore = false)
                }
        }
    }

    fun syncLikes() = viewModelScope.launch {
        runCatching { repo.refreshLikedIds() }
    }

    fun toggleLike(trackId: String) = viewModelScope.launch {
        val result = if (trackId in repo.likedIds.value) repo.unlike(trackId) else repo.like(trackId)
        result.onFailure {
            _state.value = _state.value.copy(likeError = it.message ?: "Ошибка при изменении лайка")
        }
        refresh()
    }

    fun clearLikeError() {
        _state.value = _state.value.copy(likeError = null)
    }
}
