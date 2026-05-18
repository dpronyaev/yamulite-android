package dev.pdv.yamulite.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pdv.yamulite.data.auth.AuthRepository
import dev.pdv.yamulite.data.settings.SettingsStore
import dev.pdv.yamulite.data.settings.ThemePreference
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthGate(val ready: Boolean, val token: String?)

@HiltViewModel
class AppViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val settingsStore: SettingsStore,
) : ViewModel() {
    val gate: StateFlow<AuthGate> = authRepo.tokenFlow
        .map { AuthGate(ready = true, token = it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AuthGate(ready = false, token = null))

    val theme: StateFlow<ThemePreference> = settingsStore.theme
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemePreference.System)

    fun logout() = viewModelScope.launch { authRepo.logout() }
}
