package com.acite.katahana.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acite.katahana.ai.FullStrengthBot
import com.acite.katahana.ai.HumanBot
import com.acite.katahana.ai.QualityMark
import com.acite.katahana.ai.RankBot
import com.acite.katahana.ai.bandForLoss
import com.acite.katahana.ai.recentQualities
import com.acite.katahana.domain.AiStyle
import com.acite.katahana.domain.EvalGraphMode
import com.acite.katahana.domain.EvalSample
import com.acite.katahana.domain.EvalView
import com.acite.katahana.domain.GameConfig
import com.acite.katahana.domain.GameSession
import com.acite.katahana.domain.GameTree
import com.acite.katahana.domain.Node
import com.acite.katahana.domain.Move
import com.acite.katahana.domain.PlayMode
import com.acite.katahana.domain.PlayResult
import com.acite.katahana.domain.Point
import com.acite.katahana.domain.QualityStats
import com.acite.katahana.domain.SessionSnapshot
import com.acite.katahana.domain.StoneColor
import com.acite.katahana.domain.TreeLayout
import com.acite.katahana.domain.layout
import com.acite.katahana.domain.nodeById
import com.acite.katahana.domain.qualityCounts
import com.acite.katahana.domain.samplesFrom
import com.acite.katahana.engine.AnalysisClient
import com.acite.katahana.engine.Candidate
import com.acite.katahana.engine.EnginePhase
import com.acite.katahana.engine.EngineStatus
import com.acite.katahana.engine.AnalysisResponse
import com.acite.katahana.engine.LiveAnalysis
import com.acite.katahana.engine.MoveInfo
import com.acite.katahana.engine.pointsLost
import com.acite.katahana.engine.DEAD_MIN_VISITS
import com.acite.katahana.engine.classifyDead
import com.acite.katahana.engine.heldOwnership
import com.acite.katahana.engine.selectCandidates
import com.acite.katahana.engine.toBlackView
import com.acite.katahana.epochMillis
import com.acite.katahana.recents.PersistedEval
import com.acite.katahana.recents.RecentGame
import com.acite.katahana.recents.RecentGamesRepository
import com.acite.katahana.recents.defaultRecentTitle
import com.acite.katahana.recents.toRecentAiStyle
import com.acite.katahana.recents.toRecentMode
import com.acite.katahana.settings.OwnershipStyle
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
import kotlinx.coroutines.flow.first
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
    val evalSamples: List<EvalSample> = emptyList(),
    val qualityStats: QualityStats = QualityStats(),
    val evalGraphMode: EvalGraphMode = EvalGraphMode.Score,
    val tree: TreeLayout = TreeLayout(emptyList(), "", 0, 0),
    val ownership: List<Double> = emptyList(),
    val deadPoints: Set<Point> = emptySet(),
    val reviewProgress: ReviewProgress? = null,
    val dirty: Boolean = false,
    val canSave: Boolean = false,
) {
    /** Ghost stone under the pointer. Hidden when the human cannot place. */
    val preview: Point?
        get() = if (snapshot.ended || snapshot.aiToPlay) null else selected ?: hover
}

enum class SaveOutcome {
    Saved,
    NeedsName,
    Failed,
}

data class ReviewProgress(
    val done: Int,
    val total: Int,
    val running: Boolean,
)

private data class StoredEval(
    val blackWinrate: Double,
    val blackScoreLead: Double,
    val visits: Int,
    val moveInfos: List<MoveInfo>,
    val toPlay: StoneColor,
    val ownership: List<Double> = emptyList(),
    val pointsLost: Double? = null,
    val hasView: Boolean = true,
) {
    fun toView(): EvalView = EvalView(blackWinrate, blackScoreLead, hasView)
}

@AssistedInject
class SessionViewModel(
    @Assisted val config: GameConfig,
    @Assisted val loadedTree: GameTree?,
    @Assisted private val recentId: String?,
    @Assisted private val recentTitle: String?,
    private val settings: SettingsRepository,
    private val analysis: AnalysisClient,
    private val recents: RecentGamesRepository,
) : ViewModel() {
    private val session = GameSession(
        config,
        loadedTree ?: recentId?.let { recents.open(it)?.tree },
    )
    private val sessionId = Random.nextLong().toULong().toString(16)
    private val nodeEvals = mutableMapOf<String, StoredEval>()
    private var aiJob: Job? = null
    private var reviewJob: Job? = null
    private var navEpoch = 0
    private var deadOwnershipNodeId: String? = null
    private var boundId: String? = recentId
    private var boundTitle: String = recentTitle?.takeIf { it.isNotBlank() } ?: defaultRecentTitle(config)
    private var boundCreatedAt: Long? = null
    private var savedSgf: String? = null
    private var savedPath: List<Int>? = null

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
    val showOwnership: StateFlow<Boolean> = settings.showOwnership.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(1_000),
        false,
    )
    val showDeadStones: StateFlow<Boolean> = settings.showDeadStones.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(1_000),
        false,
    )
    val ownershipStyle: StateFlow<OwnershipStyle> = settings.ownershipStyle.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(1_000),
        OwnershipStyle.Default,
    )
    val drawerAcrylic: StateFlow<Float> = settings.drawerAcrylic.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(1_000),
        0.55f,
    )
    val quality: StateFlow<QualityThresholds> = settings.quality.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(1_000),
        QualityThresholds(),
    )
    private val playVisits: StateFlow<Int> = settings.playVisits.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        400,
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

    fun setShowOwnership(value: Boolean) {
        viewModelScope.launch { settings.setShowOwnership(value) }
    }

    fun setShowDeadStones(value: Boolean) {
        viewModelScope.launch { settings.setShowDeadStones(value) }
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
        if (boundId != null) {
            savedSgf = sgfText()
            savedPath = session.tree.childPath()
            val existing = recents.games.value.find { it.id == boundId }
            boundCreatedAt = existing?.createdAt
            if (existing != null && boundTitle.isBlank()) boundTitle = existing.title
            existing?.evals?.let(::restoreEvals)
        }
        refreshSaveFlags()
        _state.update { it.withAnalysis() }
        viewModelScope.launch {
            quality.collect { _state.update { state -> state.withAnalysis() } }
        }
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
                    _state.update { it.withAnalysis() }
                    return@collect
                }
                _state.update {
                    it.copy(
                        blackWinrate = live.blackWinrate,
                        blackScoreLead = live.blackScoreLead,
                        visits = live.visits,
                        candidates = live.candidates,
                        analyzing = live.isDuringSearch,
                        ownership = heldOwnership(
                            it.ownership,
                            live.ownership,
                            session.tree.size,
                        ),
                        deadPoints = resolveDead(
                            live.nodeId,
                            live.ownership,
                            live.visits,
                            it.deadPoints,
                        ),
                    ).withAnalysis()
                }
            }
        }
        afterPositionChange()
    }

    fun setEvalGraphMode(mode: EvalGraphMode) {
        if (_state.value.evalGraphMode == mode) return
        _state.update { it.copy(evalGraphMode = mode) }
    }

    fun onHover(point: Point?) {
        val next = if (point != null && (session.tree.ended || isAiToPlay())) null else point
        if (_state.value.hover == next) return
        _state.update { it.copy(hover = next) }
    }

    fun onAim(point: Point?) {
        if (session.tree.ended || isAiToPlay()) return
        if (point != null && session.position.stoneAt(point) != null) return
        _state.update { it.copy(selected = point, hover = null) }
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
        cancelReviewQueue()
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

    fun suggestedTitle(): String = boundTitle

    suspend fun save(): SaveOutcome {
        val id = boundId ?: return SaveOutcome.NeedsName
        return writeRecent(id, boundTitle, newCopy = false)
    }

    suspend fun saveAs(name: String): SaveOutcome {
        val title = name.trim().ifBlank { defaultRecentTitle(config) }
        return writeRecent(newRecentId(), title, newCopy = true)
    }

    private suspend fun writeRecent(id: String, title: String, newCopy: Boolean): SaveOutcome {
        val now = epochMillis()
        val created = if (newCopy) now else (boundCreatedAt ?: now)
        val game = RecentGame(
            id = id,
            title = title,
            savedAt = now,
            createdAt = created,
            moveNumber = session.tree.current.moveNumber,
            boardSize = config.boardSize,
            komi = config.komi,
            mode = config.toRecentMode(),
            rankKyu = config.rankKyu,
            humanPlaysBlack = config.humanPlaysBlack,
            aiStyle = config.toRecentAiStyle(),
            sgf = sgfText(),
            currentPath = session.tree.childPath(),
            evals = dumpEvals(),
        )
        return runCatching {
            recents.upsert(game)
            boundId = id
            boundTitle = title
            boundCreatedAt = created
            savedSgf = game.sgf
            savedPath = game.currentPath
            refreshSaveFlags()
            SaveOutcome.Saved
        }.getOrElse { SaveOutcome.Failed }
    }

    private fun newRecentId(): String = Random.nextLong().toULong().toString(16)

    private fun hasContent(): Boolean = session.tree.root.children.isNotEmpty()

    private fun computeDirty(): Boolean {
        if (boundId == null) return hasContent()
        return sgfText() != savedSgf || session.tree.childPath() != savedPath
    }

    private fun refreshSaveFlags() {
        _state.update {
            it.copy(dirty = computeDirty(), canSave = boundId != null || hasContent())
        }
    }

    private fun aiPlayerName(): String = when {
        config.mode != PlayMode.HumanVsAi -> "White"
        config.aiStyle == AiStyle.Full -> "KataHana Full"
        else -> "KataHana ${com.acite.katahana.domain.rankLabel(config.rankKyu)}"
    }

    private fun commit(point: Point) {
        cancelReviewQueue()
        snapshotLiveToCurrentNode()
        if (session.play(point) is PlayResult.Ok) publish()
    }

    private fun publish() {
        val eval = nodeEvals[session.tree.current.id]
        _state.update {
            it.copy(
                snapshot = session.snapshot(),
                selected = null,
                hover = null,
                candidates = eval?.let { stored ->
                    selectCandidates(stored.moveInfos, session.tree.size, stored.toPlay)
                } ?: emptyList(),
                blackWinrate = eval?.blackWinrate,
                blackScoreLead = eval?.blackScoreLead,
                visits = eval?.visits ?: 0,
                ownership = heldOwnership(it.ownership, eval?.ownership ?: emptyList(), session.tree.size),
                deadPoints = resolveDead(
                    session.tree.current.id,
                    eval?.ownership ?: emptyList(),
                    eval?.visits ?: 0,
                    it.deadPoints,
                ),
                aiThinking = false,
                aiError = null,
                tree = session.tree.layout(),
                dirty = computeDirty(),
                canSave = boundId != null || hasContent(),
            ).withAnalysis()
        }
        afterPositionChange()
    }

    private fun afterPositionChange() {
        if (isAiToPlay() && analysis.status.value.online) {
            cancelReviewQueue()
            playAi()
        } else if (!isAiToPlay() && !reviewRunning()) {
            requestLive()
        }
    }

    private fun bumpNav() {
        navEpoch++
        aiJob?.cancel()
    }

    private fun playAi() {
        val epoch = navEpoch
        cancelReviewQueue()
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            _state.update { it.copy(aiThinking = true, aiError = null) }
            try {
                val toPlay = session.position.toPlay
                val nodeId = session.tree.current.id
                val move = when (config.aiStyle) {
                    AiStyle.Human -> {
                        val response = analysis.queryHuman(sessionId, session.tree, config.rankKyu)
                        storeEval(nodeId, response, toPlay)
                        _state.update { it.withAnalysis() }
                        HumanBot.choose(response.humanPolicy, session.position, Random.Default)
                    }
                    AiStyle.Rank -> {
                        val response = analysis.queryRank(sessionId, session.tree)
                        storeEval(nodeId, response, toPlay)
                        _state.update { it.withAnalysis() }
                        RankBot.choose(response.policy, session.position, config.rankKyu, Random.Default).move
                    }
                    AiStyle.Full -> {
                        val response = analysis.queryGenmove(sessionId, session.tree)
                        storeEval(nodeId, response, toPlay)
                        _state.update { it.withAnalysis() }
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
        if (!analysis.status.value.online) return
        if (hasCompleteEval(session.tree.current.id)) return
        analysis.analyzeLive(sessionId, session.tree)
    }

    private fun hasCompleteEval(nodeId: String): Boolean {
        val eval = nodeEvals[nodeId] ?: return false
        val n = session.tree.size * session.tree.size
        if (eval.ownership.size != n) return false
        return eval.visits >= playVisits.value
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
                blackWinrate = live.blackWinrate,
                blackScoreLead = live.blackScoreLead,
                visits = live.visits,
                moveInfos = live.moveInfos,
                toPlay = live.toPlay,
                ownership = live.ownership,
            ),
        )
    }

    private fun rememberEval(nodeId: String, eval: StoredEval) {
        val prev = nodeEvals[nodeId]
        val keep = when {
            prev == null -> true
            prev.visits > eval.visits -> false
            prev.visits == eval.visits && prev.ownership.isNotEmpty() && eval.ownership.isEmpty() -> false
            else -> true
        }
        if (!keep) return
        val node = session.tree.nodeById(nodeId)
        val loss = node?.let { lossInto(it, eval) } ?: eval.pointsLost ?: prev?.pointsLost
        nodeEvals[nodeId] = eval.copy(pointsLost = loss, hasView = true)
        if (node != null) refreshChildLosses(node)
    }

    private fun lossInto(node: Node, eval: StoredEval): Double? {
        val parent = node.parent ?: return eval.pointsLost
        val parentEval = nodeEvals[parent.id] ?: return eval.pointsLost
        val place = node.move as? Move.Place
        val gtp = place?.point?.toGtp(session.tree.size)
        val info = gtp?.let { key -> parentEval.moveInfos.find { it.move.equals(key, ignoreCase = true) } }
        return when {
            info != null -> {
                val bestLead = parentEval.moveInfos.minByOrNull { it.order }?.scoreLead
                    ?: parentEval.blackScoreLead
                pointsLost(bestLead, info.scoreLead, parentEval.toPlay)
            }
            eval.hasView && parentEval.hasView ->
                pointsLost(parentEval.blackScoreLead, eval.blackScoreLead, parentEval.toPlay)
            else -> eval.pointsLost
        }
    }

    private fun refreshChildLosses(parent: Node) {
        val parentEval = nodeEvals[parent.id] ?: return
        if (parentEval.moveInfos.isEmpty()) return
        val size = session.tree.size
        val bestLead = parentEval.moveInfos.minByOrNull { it.order }?.scoreLead
            ?: parentEval.blackScoreLead
        for (child in parent.children) {
            val place = child.move as? Move.Place ?: continue
            val gtp = place.point.toGtp(size)
            val info = parentEval.moveInfos.find { it.move.equals(gtp, ignoreCase = true) } ?: continue
            val loss = pointsLost(bestLead, info.scoreLead, parentEval.toPlay)
            val existing = nodeEvals[child.id]
            if (existing != null) {
                nodeEvals[child.id] = existing.copy(pointsLost = loss)
            } else {
                nodeEvals[child.id] = StoredEval(
                    blackWinrate = 0.0,
                    blackScoreLead = 0.0,
                    visits = 0,
                    moveInfos = emptyList(),
                    toPlay = child.position.toPlay,
                    pointsLost = loss,
                    hasView = false,
                )
            }
        }
    }

    private fun storeEval(nodeId: String, response: AnalysisResponse, toPlay: StoneColor) {
        val root = response.rootInfo
        val view = toBlackView(root?.winrate ?: 0.0, root?.scoreLead ?: 0.0)
        rememberEval(
            nodeId,
            StoredEval(
                blackWinrate = view.winrate,
                blackScoreLead = view.scoreLead,
                visits = root?.visits ?: 0,
                moveInfos = response.moveInfos,
                toPlay = toPlay,
                ownership = response.ownership,
            ),
        )
    }

    fun analyzeGame() {
        if (reviewRunning()) {
            cancelReviewQueue()
            return
        }
        if (!analysis.status.value.online) return
        reviewJob = viewModelScope.launch {
            val visitsNeeded = settings.reviewVisits.first()
            val size = session.tree.size
            val line = session.tree.preferredLine()
            val pending = line.filter { node -> needsReview(node, visitsNeeded, size) }
            if (pending.isEmpty()) {
                _state.update { it.copy(reviewProgress = ReviewProgress(line.size, line.size, false)) }
                return@launch
            }
            _state.update { it.copy(reviewProgress = ReviewProgress(0, pending.size, true)) }
            try {
                pending.forEachIndexed { index, node ->
                    val response = analysis.queryReview(sessionId, session.tree, node)
                    storeEval(node.id, response, node.position.toPlay)
                    val eval = nodeEvals[node.id]
                    val isCurrent = session.tree.current.id == node.id
                    _state.update { state ->
                        state.copy(
                            reviewProgress = ReviewProgress(index + 1, pending.size, true),
                            blackWinrate = if (isCurrent) eval?.blackWinrate else state.blackWinrate,
                            blackScoreLead = if (isCurrent) eval?.blackScoreLead else state.blackScoreLead,
                            visits = if (isCurrent) eval?.visits ?: state.visits else state.visits,
                            candidates = if (isCurrent && eval != null) {
                                selectCandidates(eval.moveInfos, size, eval.toPlay)
                            } else {
                                state.candidates
                            },
                            ownership = if (isCurrent) {
                                heldOwnership(state.ownership, eval?.ownership ?: emptyList(), size)
                            } else {
                                state.ownership
                            },
                            deadPoints = if (isCurrent) {
                                resolveDead(
                                    node.id,
                                    eval?.ownership ?: emptyList(),
                                    eval?.visits ?: 0,
                                    state.deadPoints,
                                )
                            } else {
                                state.deadPoints
                            },
                        ).withAnalysis()
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        reviewProgress = it.reviewProgress?.copy(running = false),
                        aiError = e.message?.take(160),
                    )
                }
                return@launch
            }
            _state.update { it.copy(reviewProgress = it.reviewProgress?.copy(running = false)) }
            if (!isAiToPlay()) requestLive()
        }
    }

    private fun needsReview(node: Node, visitsNeeded: Int, size: Int): Boolean {
        val eval = nodeEvals[node.id] ?: return true
        if (eval.visits < visitsNeeded) return true
        return eval.ownership.size != size * size
    }

    private fun reviewRunning(): Boolean = reviewJob?.isActive == true

    private fun cancelReviewQueue() {
        val job = reviewJob ?: return
        reviewJob = null
        analysis.cancelLive(sessionId)
        job.cancel()
        _state.update { it.copy(reviewProgress = it.reviewProgress?.copy(running = false)) }
    }

    /**
     * Dead marks use only ownership computed for [nodeId].
     * Stale/held heat is ignored so a drop into enemy ground does not flash dead.
     */
    private fun resolveDead(
        nodeId: String,
        ownership: List<Double>,
        visits: Int,
        previous: Set<Point>,
    ): Set<Point> {
        val size = session.tree.size
        val cells = session.position.cells
        val usable = ownership.size == size * size && visits >= DEAD_MIN_VISITS
        if (!usable) {
            return classifyDead(cells, size, emptyList(), previous, sameNode = false)
        }
        val same = nodeId == deadOwnershipNodeId
        val next = classifyDead(cells, size, ownership, previous, sameNode = same)
        deadOwnershipNodeId = nodeId
        return next
    }

    private fun SessionUiState.withAnalysis(): SessionUiState {
        val boardMarks = collectQualities(session.tree.nodesFromRoot())
        val lineMarks = collectQualities(session.tree.preferredLine())
        return copy(
            qualities = recentQualities(boardMarks),
            evalSamples = samplesFrom(session.tree.preferredLine()) { id -> nodeEvals[id]?.toView() },
            qualityStats = qualityCounts(lineMarks),
        )
    }

    private fun collectQualities(line: List<Node>): List<QualityMark> {
        val thresholds = quality.value
        val marks = ArrayList<QualityMark>()
        val size = session.tree.size
        for (i in 1 until line.size) {
            val parent = line[i - 1]
            val node = line[i]
            val place = node.move as? Move.Place ?: continue
            val eval = nodeEvals[parent.id]
            val childEval = nodeEvals[node.id]
            val gtp = place.point.toGtp(size)
            val info = eval?.moveInfos?.find { it.move.equals(gtp, ignoreCase = true) }
            val persisted = childEval?.pointsLost
            val loss = when {
                eval != null && info != null -> {
                    val bestLead = eval.moveInfos.minByOrNull { it.order }?.scoreLead
                        ?: eval.blackScoreLead
                    pointsLost(bestLead, info.scoreLead, eval.toPlay)
                }
                persisted != null -> persisted
                eval != null && childEval != null && eval.hasView && childEval.hasView ->
                    pointsLost(eval.blackScoreLead, childEval.blackScoreLead, eval.toPlay)
                else -> continue
            }
            val visits = info?.visits?.takeIf { it > 0 } ?: childEval?.visits ?: eval?.visits ?: 0
            marks += QualityMark(
                point = place.point,
                pointsLost = loss,
                visits = visits,
                band = bandForLoss(loss, thresholds),
                color = place.color,
            )
        }
        return marks
    }

    private fun restoreEvals(records: List<PersistedEval>) {
        for (rec in records) {
            val node = session.tree.nodeAtPath(rec.path) ?: continue
            val toPlay = if (rec.toPlay.equals("W", ignoreCase = true)) {
                StoneColor.White
            } else {
                StoneColor.Black
            }
            val hasView = rec.blackWinrate != null && rec.blackScoreLead != null
            nodeEvals[node.id] = StoredEval(
                blackWinrate = rec.blackWinrate ?: 0.0,
                blackScoreLead = rec.blackScoreLead ?: 0.0,
                visits = rec.visits,
                moveInfos = emptyList(),
                toPlay = toPlay,
                pointsLost = rec.pointsLost,
                hasView = hasView,
            )
        }
    }

    private fun dumpEvals(): List<PersistedEval> {
        val out = ArrayList<PersistedEval>()
        fun walk(node: Node) {
            val eval = nodeEvals[node.id]
            if (eval != null) {
                out += PersistedEval(
                    path = node.pathFromRoot(),
                    blackWinrate = eval.blackWinrate.takeIf { eval.hasView },
                    blackScoreLead = eval.blackScoreLead.takeIf { eval.hasView },
                    visits = eval.visits,
                    toPlay = if (eval.toPlay == StoneColor.Black) "B" else "W",
                    pointsLost = eval.pointsLost,
                )
            }
            node.children.forEach(::walk)
        }
        walk(session.tree.root)
        return out
    }

    private fun flushBoundEvals() {
        val id = boundId ?: return
        val existing = recents.games.value.find { it.id == id } ?: return
        recents.upsertSync(existing.copy(evals = dumpEvals()))
    }

    fun leave() {
        navEpoch++
        aiJob?.cancel()
        cancelReviewQueue()
        analysis.cancelLive(sessionId)
        flushBoundEvals()
    }

    override fun onCleared() {
        leave()
        super.onCleared()
    }

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppScope::class)
    interface Factory : ManualViewModelAssistedFactory {
        fun create(
            config: GameConfig,
            loadedTree: GameTree?,
            recentId: String?,
            recentTitle: String?,
        ): SessionViewModel
    }
}
