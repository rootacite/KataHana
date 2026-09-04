package com.acite.katahana.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acite.katahana.domain.GameConfig
import com.acite.katahana.recents.LoadedRecent
import com.acite.katahana.recents.RecentGame
import com.acite.katahana.recents.RecentGamesRepository
import com.acite.katahana.settings.SettingsRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
class HomeViewModel(
    private val settings: SettingsRepository,
    private val recents: RecentGamesRepository,
) : ViewModel() {
    val lastGame: StateFlow<GameConfig> = settings.lastGame.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(1_000), GameConfig(),
    )
    val recentGames: StateFlow<List<RecentGame>> = recents.games

    fun saveLastGame(value: GameConfig) {
        viewModelScope.launch { settings.setLastGame(value) }
    }

    fun removeRecent(id: String) {
        viewModelScope.launch { recents.remove(id) }
    }

    fun openRecent(id: String): LoadedRecent? = recents.open(id)
}
