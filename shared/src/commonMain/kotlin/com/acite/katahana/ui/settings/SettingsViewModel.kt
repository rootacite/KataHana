package com.acite.katahana.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acite.katahana.engine.AnalysisClient
import com.acite.katahana.engine.EngineProfile
import com.acite.katahana.engine.EngineStatus
import com.acite.katahana.engine.TestResult
import com.acite.katahana.domain.GameConfig
import com.acite.katahana.settings.OwnershipStyle
import com.acite.katahana.settings.QualityThresholds
import com.acite.katahana.settings.SettingsRepository
import com.acite.katahana.ui.Copy
import com.acite.katahana.ui.theme.Appearance
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
class SettingsViewModel(
    private val repo: SettingsRepository,
    private val analysis: AnalysisClient,
) : ViewModel() {
    val confirmMove: StateFlow<Boolean> = repo.confirmMove.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(1_000), false,
    )
    val showCoords: StateFlow<Boolean> = repo.showCoords.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(1_000), true,
    )
    val appearanceId: StateFlow<String> = repo.appearanceId.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(1_000), Appearance.DEFAULT_ID,
    )
    val ownershipStyle: StateFlow<OwnershipStyle> = repo.ownershipStyle.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(1_000), OwnershipStyle.Default,
    )
    val engineName: StateFlow<String> = repo.engineName.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(1_000), "KataGo",
    )
    val engineUrl: StateFlow<String> = repo.engineUrl.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(1_000), EngineProfile.DEFAULT_URL,
    )
    val engineStatus: StateFlow<EngineStatus> = analysis.status
    private val _testBusy = MutableStateFlow(false)
    val testBusy: StateFlow<Boolean> = _testBusy.asStateFlow()
    private val _testMessage = MutableStateFlow<String?>(null)
    val testMessage: StateFlow<String?> = _testMessage.asStateFlow()
    val engineToken: StateFlow<String> = repo.engineToken.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(1_000), "",
    )
    val playVisits: StateFlow<Int> = repo.playVisits.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(1_000), 400,
    )
    val reviewVisits: StateFlow<Int> = repo.reviewVisits.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(1_000), 400,
    )
    val quality: StateFlow<QualityThresholds> = repo.quality.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(1_000), QualityThresholds(),
    )
    val lastGame: StateFlow<GameConfig> = repo.lastGame.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(1_000), GameConfig(),
    )

    fun saveLastGame(value: GameConfig) = launch { repo.setLastGame(value) }

    fun setConfirmMove(value: Boolean) = launch { repo.setConfirmMove(value) }
    fun setShowCoords(value: Boolean) = launch { repo.setShowCoords(value) }
    fun setAppearanceId(value: String) = launch { repo.setAppearanceId(value) }
    fun setOwnershipStyle(value: OwnershipStyle) = launch { repo.setOwnershipStyle(value) }
    fun setEngineName(value: String) = launch { repo.setEngineName(value) }
    fun setEngineUrl(value: String) = launch { repo.setEngineUrl(value) }
    fun setEngineToken(value: String) = launch { repo.setEngineToken(value) }
    fun setPlayVisits(value: Int) = launch { repo.setPlayVisits(value) }
    fun setReviewVisits(value: Int) = launch { repo.setReviewVisits(value) }
    fun setQuality(value: QualityThresholds) = launch { repo.setQuality(value) }

    fun testConnection() {
        if (_testBusy.value) return
        viewModelScope.launch {
            _testBusy.value = true
            _testMessage.value = Copy.testingConnection
            _testMessage.value = when (val result = analysis.testConnection()) {
                is TestResult.Ok -> "${Copy.connected} · ${result.visits} ${Copy.visitsLabel}"
                is TestResult.Fail -> result.message
            }
            _testBusy.value = false
        }
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
