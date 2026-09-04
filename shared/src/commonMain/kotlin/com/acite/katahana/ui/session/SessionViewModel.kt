package com.acite.katahana.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acite.katahana.ai.FullStrengthBot
import com.acite.katahana.ai.QualityMark
import com.acite.katahana.ai.RankBot
import com.acite.katahana.ai.bandForLoss
import com.acite.katahana.ai.recentQualities
import com.acite.katahana.domain.AiStyle
import com.acite.katahana.domain.GameConfig
import com.acite.katahana.domain.GameSession
import com.acite.katahana.domain.GameTree
import com.acite.katahana.domain.Move
import com.acite.katahana.domain.PlayMode
import com.acite.katahana.domain.PlayResult
import com.acite.katahana.domain.Point
import com.acite.katahana.domain.SessionSnapshot
import com.acite.katahana.domain.StoneColor
import com.acite.katahana.domain.TreeLayout
import com.acite.katahana.domain.layout
import com.acite.katahana.engine.AnalysisClient
import com.acite.katahana.engine.Candidate
import com.acite.katahana.engine.EnginePhase
import com.acite.katahana.engine.EngineStatus
import com.acite.katahana.engine.AnalysisResponse
import com.acite.katahana.engine.LiveAnalysis
import com.acite.katahana.engine.MoveInfo
import com.acite.katahana.engine.pointsLost
import com.acite.katahana.engine.toBlackView
import com.acite.katahana.settings.QualityThresholds
import com.acite.katahana.settings.SettingsRepository
import com.acite.katahana.sgf.writeSgf
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SessionUiState(
    val snapshot: SessionSnapshot,
    val selected: Point? = null,
    val hover: Point? = null,
    val engineStatus: EngineStatus = EngineStatus(),
    val blackWinrate: Double? = null,
    val blackScoreLead: Double? = null,
    val visits: Int = 0,
    val candidates: List<Candidate> = emptyList(),
    val analyzing: Boolean = false,
    val aiThinking: Boolean = false,
    val aiError: String? = null,
    val qualities: List<QualityMark> = emptyList(),
    val tree: TreeLayout = TreeLayout(emptyList(), "", 0, 0),
) {
    val preview: Point? get() = selected ?: hover
}

private data class StoredEval(
    val blackScoreLead: Double,
    val visits: Int,
    val moveInfos: List<MoveInfo>,
    val toPlay: StoneColor,
)

@AssistedInject
class SessionViewModel(
    @Assisted val config: GameConfig,
    @Assisted val loadedTree: GameTree?,
    private val settings: SettingsRepository,
    private val analysis: AnalysisClient,
) : ViewModel() {
    private val session = GameSession(config, loadedTree)
    private val sessionId = Random.nextLong().toULong().toString(16)
    private val nodeEvals = mutableMapOf<String, StoredEval>()
    private var aiJob: Job? = null
    private var navEpoch = 0

    val confirmMove: StateFlow<Boolean> = settings.confirmMove.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(1_000),
        false,
    )
    val showCoords: StateFlow<Boolean> = settings.showCoords.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(1_000),
        true,
    )
    val showCandidates: StateFlow<Boolean> = settings.showCandidates.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(1_000),
        true,
    )
    val showQuality: StateFlow<Boolean> = settings.showQuality.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(1_000),
        true,
    )
    val showConnections: StateFlow<Boolean> = settings.showConnections.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(1_000),
        false,
    )
    val quality: StateFlow<QualityThresholds> = settings.quality.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(1_000),
        QualityThresholds(),
    )

    fun setShowCandidates(value: Boolean) {
        viewModelScope.launch { settings.setShowCandidates(value) }
    }

    fun setShowQuality(value: Boolean) {
        viewModelScope.launch { settings.setShowQuality(value) }
    }

    fun setShowConnections(value: Boolean) {
        viewModelScope.launch { settings.setShowConnections(value) }
    }

    fun setShowCoords(value: Boolean) {
        viewModelScope.launch { settings.setShowCoords(value) }
    }

    private val _state = MutableStateFlow(
        SessionUiState(
            snapshot = session.snapshot(),
            engineStatus = analysis.status.value,
            tree = session.tree.layout(),
        ),
    )
    val state: StateFlow<SessionUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            analysis.status.collect { status ->
                val becameOnline = status.online && !_state.value.engineStatus.online
                _state.update {
                    it.copy(engineStatus = status, analyzing = status.phase == EnginePhase.Analyzing)
                }
                if (becameOnline) afterPositionChange()
            }
        }
        viewModelScope.launch {
            analysis.live.collect { live ->
                if (live == null) return@collect
                if (live.sessionId != sessionId) return@collect
                rememberLive(live)
                if (live.nodeId != session.tree.current.id) {
                    _state.update { it.copy(qualities = refreshQualities()) }
                    return@collect
                }
                _state.update {
                    it.copy(
                        blackWinrate = live.blackWinrate,
                        blackScoreLead = live.blackScoreLead,
                        visits = live.visits,
                        candidates = live.candidates,
                        analyzing = live.isDuringSearch,
                        qualities = refreshQualities(),
                    )
                }
            }
        }
        afterPositionChange()
    }

    fun onHover(point: Point?) {
        _state.update { it.copy(hover = point) }
    }

    fun onActivate(point: Point, isTouch: Boolean) {
        if (session.tree.ended || isAiToPlay()) return
        if (session.position.stoneAt(point) != null) return
        val twoStep = isTouch || confirmMove.value
        if (twoStep) {
            if (_state.value.selected == point) {
                commit(point)
            } else {
                _state.update { it.copy(selected = point, hover = null) }
            }
        } else {
            commit(point)
        }
    }

    fun confirmSelected() {
        if (isAiToPlay()) return
        val point = _state.value.selected ?: return
        commit(point)
    }

    fun pass() {
        if (isAiToPlay()) return
        snapshotLiveToCurrentNode()
        if (session.pass() is PlayResult.Ok) publish()
    }

    fun undo() {
        bumpNav()
        if (session.undo()) publish()
    }

    fun redo() {
        bumpNav()
        if (session.redo()) publish()
    }

    fun cycleVariation(delta: Int) {
        bumpNav()
        if (session.cycleVariation(delta)) publish()
    }

    fun goToNode(id: String) {
        if (id == session.tree.current.id) return
        bumpNav()
        if (session.goTo(id)) publish()
    }

    fun sgfText(): String {
        val black = if (config.humanPlaysBlack) "Human" else aiPlayerName()
        val white = if (config.humanPlaysBlack) aiPlayerName() else "Human"
        return writeSgf(session.tree, config, black, white)
    }

    fun sgfFileName(): String = "katahana-${session.tree.size}x${session.tree.size}.sgf"

    private fun aiPlayerName(): String = when {
        config.mode != PlayMode.HumanVsAi -> "White"
        config.aiStyle == AiStyle.Full -> "KataHana Full"
        else -> "KataHana ${com.acite.katahana.domain.rankLabel(config.rankKyu)}"
    }

    private fun commit(point: Point) {
        snapshotLiveToCurrentNode()
        if (session.play(point) is PlayResult.Ok) publish()
    }

    private fun publish() {
        _state.update {
            it.copy(
                snapshot = session.snapshot(),
                selected = null,
                hover = null,
                candidates = emptyList(),
                aiThinking = false,
                aiError = null,
                qualities = refreshQualities(),
                tree = session.tree.layout(),
            )
        }
        afterPositionChange()
    }

    private fun afterPositionChange() {
        if (isAiToPlay() && analysis.status.value.online) {
            playAi()
        } else if (!isAiToPlay()) {
            requestLive()
        }
    }

    private fun bumpNav() {
        navEpoch++
        aiJob?.cancel()
    }

    private fun playAi() {
        val epoch = navEpoch
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            _state.update { it.copy(aiThinking = true, aiError = null) }
            try {
                val toPlay = session.position.toPlay
                val nodeId = session.tree.current.id
                val move = when (config.aiStyle) {
                    AiStyle.Rank -> {
                        val response = analysis.queryRank(sessionId, session.tree)
                        storeEval(nodeId, response, toPlay)
                        _state.update { it.copy(qualities = refreshQualities()) }
                        RankBot.choose(response.policy, session.position, config.rankKyu, Random.Default).move
                    }
                    AiStyle.Full -> {
                        val response = analysis.queryGenmove(sessionId, session.tree)
                        storeEval(nodeId, response, toPlay)
                        _state.update { it.copy(qualities = refreshQualities()) }
                        FullStrengthBot.choose(response.moveInfos, session.tree.size, toPlay)
                    }
                }
                if (epoch != navEpoch) return@launch
                if (session.tree.ended || !isAiToPlay()) return@launch
                if (session.tree.current.id != nodeId) return@launch
                when (move) {
                    is Move.Place -> session.play(move.point)
                    is Move.Pass -> session.pass()
                }
                if (epoch != navEpoch) return@launch
                publish()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (epoch != navEpoch) return@launch
                _state.update {
                    it.copy(aiThinking = false, aiError = e.message?.take(160))
                }
            }
        }
    }

    private fun isAiToPlay(): Boolean = session.aiShouldMove()

    private fun requestLive() {
        if (analysis.status.value.online) {
            analysis.analyzeLive(sessionId, session.tree)
        }
    }

    private fun snapshotLiveToCurrentNode() {
        val live = analysis.live.value ?: return
        if (live.sessionId != sessionId) return
        if (live.nodeId != session.tree.current.id) return
        rememberLive(live)
    }

    private fun rememberLive(live: LiveAnalysis) {
        rememberEval(
            live.nodeId,
            StoredEval(
                blackScoreLead = live.blackScoreLead,
                visits = live.visits,
                moveInfos = live.moveInfos,
                toPlay = live.toPlay,
            ),
        )
    }

    private fun rememberEval(nodeId: String, eval: StoredEval) {
        val prev = nodeEvals[nodeId]
        if (prev != null && prev.visits > eval.visits) return
        nodeEvals[nodeId] = eval
    }

    private fun storeEval(nodeId: String, response: AnalysisResponse, toPlay: StoneColor) {
        val root = response.rootInfo
        val view = toBlackView(root?.winrate ?: 0.0, root?.scoreLead ?: 0.0)
        rememberEval(
            nodeId,
            StoredEval(
                blackScoreLead = view.scoreLead,
                visits = root?.visits ?: 0,
                moveInfos = response.moveInfos,
                toPlay = toPlay,
            ),
        )
    }

    private fun refreshQualities(): List<QualityMark> {
        val thresholds = quality.value
        val marks = ArrayList<QualityMark>()
        val line = session.tree.nodesFromRoot()
        val size = session.tree.size
        for (i in 1 until line.size) {
            val parent = line[i - 1]
            val node = line[i]
            val place = node.move as? Move.Place ?: continue
            val eval = nodeEvals[parent.id] ?: continue
            val gtp = place.point.toGtp(size)
            val info = eval.moveInfos.find { it.move.equals(gtp, ignoreCase = true) }
            val childEval = nodeEvals[node.id]
            val loss = if (info != null) {
                val bestLead = eval.moveInfos.minByOrNull { it.order }?.scoreLead ?: eval.blackScoreLead
                pointsLost(bestLead, info.scoreLead, eval.toPlay)
            } else if (childEval != null) {
                pointsLost(eval.blackScoreLead, childEval.blackScoreLead, eval.toPlay)
            } else {
                continue
            }
            val visits = info?.visits?.takeIf { it > 0 } ?: childEval?.visits ?: eval.visits
            marks += QualityMark(
                point = place.point,
                pointsLost = loss,
                visits = visits,
                band = bandForLoss(loss, thresholds),
                color = place.color,
            )
        }
        return recentQualities(marks)
    }

    fun leave() {
        navEpoch++
        aiJob?.cancel()
        analysis.cancelLive(sessionId)
    }

    override fun onCleared() {
        leave()
        super.onCleared()
    }

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppScope::class)
    interface Factory : ManualViewModelAssistedFactory {
        fun create(config: GameConfig, loadedTree: GameTree?): SessionViewModel
    }
}
