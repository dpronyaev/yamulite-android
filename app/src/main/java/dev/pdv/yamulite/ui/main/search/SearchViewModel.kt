package dev.pdv.yamulite.ui.main.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pdv.yamulite.data.music.MusicRepository
import dev.pdv.yamulite.data.music.SearchResults
import dev.pdv.yamulite.data.music.SearchType
import dev.pdv.yamulite.data.music.dto.TrackDto
import dev.pdv.yamulite.data.playback.AudioPlayer
import dev.pdv.yamulite.data.playback.DownloadManager
import dev.pdv.yamulite.data.playback.DownloadState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val type: SearchType = SearchType.Tracks,
    val loading: Boolean = false,
    val results: SearchResults = SearchResults(),
    val error: String? = null,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repo: MusicRepository,
    private val player: AudioPlayer,
    private val downloads: DownloadManager,
) : ViewModel() {

    val downloadStates = downloads.downloads

    fun play(tracks: List<TrackDto>, startIndex: Int) = player.play(tracks, startIndex)

    fun onDownloadClick(trackId: String) {
        if (downloads.downloads.value[trackId]?.state == DownloadState.Done) {
            downloads.delete(trackId)
        } else {
            downloads.download(trackId)
        }
    }


    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()
    val likedIds = repo.likedIds

    private val queryFlow = MutableStateFlow("")
    private var inflightJob: Job? = null

    init {
        queryFlow
            .debounce(350)
            .distinctUntilChanged()
            .onEach { runSearch() }
            .launchIn(viewModelScope)
    }

    fun onQueryChange(text: String) {
        _state.value = _state.value.copy(query = text)
        queryFlow.value = text.trim()
    }

    fun onTypeChange(type: SearchType) {
        if (_state.value.type == type) return
        _state.value = _state.value.copy(type = type)
        runSearch()
    }

    fun toggleLike(trackId: String) = viewModelScope.launch {
        if (trackId in repo.likedIds.value) repo.unlike(trackId) else repo.like(trackId)
    }

    private fun runSearch() {
        val q = _state.value.query.trim()
        val type = _state.value.type
        inflightJob?.cancel()
        if (q.isBlank()) {
            _state.value = _state.value.copy(loading = false, results = SearchResults(), error = null)
            return
        }
        _state.value = _state.value.copy(loading = true, error = null)
        inflightJob = viewModelScope.launch {
            runCatching { repo.search(q, type) }
                .onSuccess {
                    _state.value = _state.value.copy(loading = false, results = it, error = null)
                }
                .onFailure {
                    _state.value = _state.value.copy(loading = false, error = it.message ?: "Ошибка поиска")
                }
        }
    }
}
