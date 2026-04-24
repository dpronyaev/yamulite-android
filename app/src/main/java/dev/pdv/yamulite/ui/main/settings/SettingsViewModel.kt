package dev.pdv.yamulite.ui.main.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pdv.yamulite.data.settings.Quality
import dev.pdv.yamulite.data.settings.SettingsStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val store: SettingsStore,
) : ViewModel() {
    val quality = store.quality.stateIn(viewModelScope, SharingStarted.Eagerly, Quality.High)

    fun setQuality(q: Quality) = viewModelScope.launch { store.setQuality(q) }
}
