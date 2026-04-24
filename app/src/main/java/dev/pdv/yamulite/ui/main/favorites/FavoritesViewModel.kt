package dev.pdv.yamulite.ui.main.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pdv.yamulite.data.music.MusicRepository
import dev.pdv.yamulite.data.music.dto.TrackDto
import dev.pdv.yamulite.data.playback.AudioPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoritesUiState(
    val loading: Boolean = false,
    val tracks: List<TrackDto> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repo: MusicRepository,
    private val player: AudioPlayer,
) : ViewModel() {

    fun play(tracks: List<TrackDto>, startIndex: Int) = player.play(tracks, startIndex)


    private val _state = MutableStateFlow(FavoritesUiState())
    val state: StateFlow<FavoritesUiState> = _state.asStateFlow()
    val likedIds = repo.likedIds

    init {
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        runCatching { repo.favorites() }
            .onSuccess { _state.value = FavoritesUiState(tracks = it) }
            .onFailure { _state.value = FavoritesUiState(error = it.message ?: "Ошибка загрузки") }
    }

    fun toggleLike(trackId: String) = viewModelScope.launch {
        if (trackId in repo.likedIds.value) repo.unlike(trackId) else repo.like(trackId)
        refresh()
    }
}
