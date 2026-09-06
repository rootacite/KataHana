package com.acite.katahana.engine

import com.acite.katahana.ai.HumanBot
import com.acite.katahana.domain.GameTree
import com.acite.katahana.domain.Move
import com.acite.katahana.domain.Node
import com.acite.katahana.domain.Point
import com.acite.katahana.domain.StoneColor
import com.acite.katahana.settings.SettingsRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.random.Random
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import kotlin.time.TimeSource

@Inject
@SingleIn(AppScope::class)
class AnalysisClient(
    private val settings: SettingsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val ws = WsClient()
    private val waitersMutex = Mutex()
    private val sendMutex = Mutex()
    private val waiters = mutableMapOf<String, QueryWaiter>()

    private val _status = MutableStateFlow(EngineStatus())
    val status: StateFlow<EngineStatus> = _status.asStateFlow()

    private val _live = MutableStateFlow<LiveAnalysis?>(null)
    val live: StateFlow<LiveAnalysis?> = _live.asStateFlow()

    private var outbound: Channel<String>? = null
    private var inFlight: InFlight? = null
    private var currentProfile: EngineProfile = EngineProfile()

    init {
        scope.launch {
            settings.engineProfile
                .distinctUntilChanged()
                .collectLatest { profile ->
                    currentProfile = profile
                    runConnection(profile)
                }
        }
    }

    fun analyzeLive(sessionId: String, tree: GameTree) {
        val profile = currentProfile
        if (profile.url.isBlank()) return
        if (!_status.value.online && _status.value.phase != EnginePhase.Connecting) return
        val nodeId = tree.current.id
        val existing = inFlight
        if (existing != null && existing.sessionId == sessionId && existing.nodeId == nodeId) return
        val nonce = Random.nextLong().toULong().toString(16)
        val query = buildLiveQuery(
            sessionId = sessionId,
            nodeId = nodeId,
            nonce = nonce,
            tree = tree,
            maxVisits = profile.playVisits,
        )
        val previous = existing
        inFlight = InFlight(
            queryId = query.id,
            sessionId = sessionId,
            nodeId = nodeId,
            boardSize = tree.size,
            toPlay = tree.current.position.toPlay,
        )
        _status.value = EngineStatus(EnginePhase.Analyzing)
        scope.launch {
            sendMutex.withLock {
                if (inFlight?.queryId != query.id) return@withLock
                if (previous != null) {
                    sendJson(
                        analysisJson.encodeToString(
                            TerminateQuery.serializer(),
                            TerminateQuery(id = "term:$nonce", terminateId = previous.queryId),
                        ),
                    )
                }
                if (inFlight?.queryId != query.id) return@withLock
                sendJson(analysisJson.encodeToString(AnalysisQuery.serializer(), query))
            }
        }
    }

    fun cancelLive(sessionId: String) {
        val current = inFlight ?: return
        if (current.sessionId != sessionId) return
        inFlight = null
        if (_status.value.phase == EnginePhase.Analyzing) {
            _status.value = EngineStatus(EnginePhase.Ready)
        }
        scope.launch {
            sendJson(
                analysisJson.encodeToString(
                    TerminateQuery.serializer(),
                    TerminateQuery(
                        id = "term-cancel:$sessionId",
                        terminateId = current.queryId,
                    ),
                ),
            )
        }
    }

    suspend fun queryPolicy(sessionId: String, tree: GameTree): List<Double> =
        queryRank(sessionId, tree).policy

    suspend fun queryRank(sessionId: String, tree: GameTree): AnalysisResponse {
        val nonce = Random.nextLong().toULong().toString(16)
        val nodeId = tree.current.id
        val query = buildRankQuery(
            sessionId,
            nodeId,
            nonce,
            tree,
        )
        val response = sendAndAwaitFinal(
            query,
            InFlight(query.id, sessionId, nodeId, tree.size, tree.current.position.toPlay),
        )
        if (response.error != null) error(response.error)
        return response
    }

    suspend fun queryHuman(sessionId: String, tree: GameTree, rankKyu: Int): AnalysisResponse {
        val nonce = Random.nextLong().toULong().toString(16)
        val nodeId = tree.current.id
        val query = buildHumanQuery(
            sessionId,
            nodeId,
            nonce,
            tree,
            rankKyu,
        )
        val response = sendAndAwaitFinal(
            query,
            InFlight(query.id, sessionId, nodeId, tree.size, tree.current.position.toPlay),
        )
        if (response.error != null) error(response.error)
        if (response.humanPolicy.isEmpty()) error(HumanBot.MISSING_POLICY)
        return response
    }

    suspend fun queryReview(sessionId: String, tree: GameTree, node: Node): AnalysisResponse {
        val nonce = Random.nextLong().toULong().toString(16)
        val query = buildReviewQuery(
            sessionId = sessionId,
            nodeId = node.id,
            nonce = nonce,
            tree = tree,
            moves = tree.pathMoves(node),
            maxVisits = currentProfile.reviewVisits,
        )
        val response = sendAndAwaitFinal(
            query,
            InFlight(query.id, sessionId, node.id, tree.size, node.position.toPlay),
        )
        if (response.error != null) error(response.error)
        return response
    }

    suspend fun queryForecast(sessionId: String, tree: GameTree, origin: Point): AnalysisResponse {
        val nonce = Random.nextLong().toULong().toString(16)
        val nodeId = tree.current.id
        val query = buildForecastQuery(
            sessionId,
            nodeId,
            nonce,
            tree,
            origin,
            currentProfile.reviewVisits,
        )
        val response = sendAndAwaitFinal(
            query,
            InFlight(
                query.id,
                sessionId,
                nodeId,
                tree.size,
                tree.current.position.toPlay,
                publishLive = false,
            ),
        )
        if (response.error != null) error(response.error)
        return response
    }

    suspend fun collectForecastOwnership(
        sessionId: String,
        tree: GameTree,
        continuation: List<Move>,
        onTurn: (ply: Int, ownership: List<Double>) -> Unit,
    ) {
        if (continuation.size <= 1) return
        val nonce = Random.nextLong().toULong().toString(16)
        val nodeId = tree.current.id
        val query = buildForecastOwnershipQuery(
            sessionId,
            nodeId,
            nonce,
            tree,
            continuation,
            currentProfile.reviewVisits,
        )
        val expected = query.analyzeTurns.orEmpty().toSet()
        if (expected.isEmpty()) return
        val pathLen = tree.lineMoves().size
        collectTurns(
            query,
            InFlight(
                query.id,
                sessionId,
                nodeId,
                tree.size,
                tree.current.position.toPlay,
                publishLive = false,
            ),
            expected,
        ) { turn, response ->
            val ply = turn - pathLen
            if (ply >= 1 && response.ownership.isNotEmpty()) {
                onTurn(ply, response.ownership)
            }
        }
    }

    suspend fun queryGenmove(sessionId: String, tree: GameTree): AnalysisResponse {
        val nonce = Random.nextLong().toULong().toString(16)
        val nodeId = tree.current.id
        val query = buildGenmoveQuery(
            sessionId,
            nodeId,
            nonce,
            tree,
            maxVisits = currentProfile.playVisits,
        )
        val response = sendAndAwaitFinal(
            query,
            InFlight(query.id, sessionId, nodeId, tree.size, tree.current.position.toPlay),
        )
        if (response.error != null) error(response.error)
        return response
    }

    private suspend fun sendAndAwaitFinal(query: AnalysisQuery, flight: InFlight): AnalysisResponse {
        val deferred = CompletableDeferred<AnalysisResponse>()
        waitersMutex.withLock { waiters[query.id] = QueryWaiter.OneShot(deferred) }
        try {
            awaitOnline()
            sendMutex.withLock {
                terminateLive()
                inFlight = flight
                _status.value = EngineStatus(EnginePhase.Analyzing)
                sendJson(analysisJson.encodeToString(AnalysisQuery.serializer(), query))
            }
            return withTimeout(60_000) { deferred.await() }
        } finally {
            finishQuery(query.id)
        }
    }

    private suspend fun collectTurns(
        query: AnalysisQuery,
        flight: InFlight,
        expectedTurns: Set<Int>,
        onTurn: (turn: Int, response: AnalysisResponse) -> Unit,
    ) {
        val channel = Channel<AnalysisResponse>(Channel.UNLIMITED)
        waitersMutex.withLock { waiters[query.id] = QueryWaiter.Stream(channel) }
        try {
            awaitOnline()
            sendMutex.withLock {
                terminateLive()
                inFlight = flight
                _status.value = EngineStatus(EnginePhase.Analyzing)
                sendJson(analysisJson.encodeToString(AnalysisQuery.serializer(), query))
            }
            val got = HashSet<Int>()
            val timeoutMs = 60_000L + 8_000L * expectedTurns.size
            withTimeout(timeoutMs) {
                while (got.size < expectedTurns.size) {
                    val response = channel.receive()
                    if (response.error != null) error(response.error)
                    val turn = response.turnNumber ?: continue
                    if (turn in expectedTurns && got.add(turn)) {
                        onTurn(turn, response)
                    }
                }
            }
        } finally {
            channel.close()
            finishQuery(query.id)
        }
    }

    private suspend fun finishQuery(queryId: String) {
        waitersMutex.withLock { waiters.remove(queryId) }
        if (inFlight?.queryId != queryId) return
        val nonce = Random.nextLong().toULong().toString(16)
        inFlight = null
        sendJson(
            analysisJson.encodeToString(
                TerminateQuery.serializer(),
                TerminateQuery(id = "term:$nonce", terminateId = queryId),
            ),
        )
        if (_status.value.phase == EnginePhase.Analyzing) {
            _status.value = EngineStatus(EnginePhase.Ready)
        }
    }

    private fun terminateLive() {
        val previous = inFlight ?: return
        inFlight = null
        val nonce = Random.nextLong().toULong().toString(16)
        sendJson(
            analysisJson.encodeToString(
                TerminateQuery.serializer(),
                TerminateQuery(id = "term:$nonce", terminateId = previous.queryId),
            ),
        )
    }

    suspend fun runBenchmark(
        playVisits: Int,
        onStep: (BenchmarkStep) -> Unit = {},
    ): BenchmarkResult {
        if (!_status.value.online) return BenchmarkResult.Fail("Engine offline")
        sendMutex.withLock { terminateLive() }
        _status.value = EngineStatus(EnginePhase.Analyzing)
        try {
            onStep(BenchmarkStep.Warmup)
            val warmup = queryOnce(
                buildTestQuery("bench:warmup:${nonce()}"),
                BENCH_POLICY_TIMEOUT_MS,
            )
            if (warmup.error != null) {
                return BenchmarkResult.Fail(warmup.error)
            }

            val policyMs = ArrayList<Long>(BENCH_POLICY_SAMPLES)
            repeat(BENCH_POLICY_SAMPLES) { i ->
                onStep(BenchmarkStep.Latency(i + 1, BENCH_POLICY_SAMPLES))
                val mark = TimeSource.Monotonic.markNow()
                val response = queryOnce(
                    buildBenchPolicyQuery("bench:policy:$i:${nonce()}"),
                    BENCH_POLICY_TIMEOUT_MS,
                )
                if (response.error != null) {
                    return BenchmarkResult.Fail(response.error)
                }
                policyMs += mark.elapsedNow().inWholeMilliseconds.coerceAtLeast(1L)
            }

            val searches = ArrayList<SearchSample>(BENCH_SEARCH_LADDER.size)
            for (visits in BENCH_SEARCH_LADDER) {
                onStep(BenchmarkStep.Search(visits))
                val searchMark = TimeSource.Monotonic.markNow()
                val search = queryOnce(
                    buildBenchSearchQuery("bench:search:$visits:${nonce()}", visits),
                    benchSearchTimeoutMs(visits),
                )
                if (search.error != null) {
                    return BenchmarkResult.Fail(search.error)
                }
                searches += SearchSample(
                    requestedVisits = visits,
                    actualVisits = search.rootInfo?.visits ?: 0,
                    elapsedMs = searchMark.elapsedNow().inWholeMilliseconds.coerceAtLeast(1L),
                )
            }

            onStep(BenchmarkStep.Human)
            var humanMs: Long? = null
            var humanPolicyPresent = false
            try {
                val humanMark = TimeSource.Monotonic.markNow()
                val human = queryOnce(
                    buildBenchHumanQuery("bench:human:${nonce()}"),
                    BENCH_POLICY_TIMEOUT_MS,
                )
                if (human.error == null && human.humanPolicy.isNotEmpty()) {
                    humanMs = humanMark.elapsedNow().inWholeMilliseconds.coerceAtLeast(1L)
                    humanPolicyPresent = true
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Human net is optional; latency/search still decide the verdict.
            }

            return BenchmarkResult.Ok(
                assembleReport(
                    policyMs = policyMs,
                    searches = searches,
                    playVisits = playVisits.coerceAtLeast(1),
                    humanMs = humanMs,
                    humanPolicyPresent = humanPolicyPresent,
                ),
            )
        } catch (e: TimeoutCancellationException) {
            return BenchmarkResult.Fail(e.message ?: "Timed out")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return BenchmarkResult.Fail(e.message ?: "Benchmark failed")
        } finally {
            if (_status.value.phase == EnginePhase.Analyzing) {
                _status.value = EngineStatus(EnginePhase.Ready)
            }
        }
    }

    suspend fun testConnection(): TestResult {
        val profile = settings.engineProfile.first()
        currentProfile = profile
        if (profile.url.isBlank()) return TestResult.Fail("Set a WebSocket URL first.")
        val id = "test:${Random.nextLong().toULong().toString(16)}"
        val deferred = CompletableDeferred<AnalysisResponse>()
        waitersMutex.withLock { waiters[id] = QueryWaiter.OneShot(deferred) }
        try {
            awaitOnline()
            sendJson(analysisJson.encodeToString(AnalysisQuery.serializer(), buildTestQuery(id)))
            val response = withTimeout(20_000) { deferred.await() }
            if (response.error != null) {
                val field = response.field?.let { " ($it)" } ?: ""
                return TestResult.Fail("${response.error}$field")
            }
            val visits = response.rootInfo?.visits ?: 0
            return TestResult.Ok(visits)
        } catch (e: Exception) {
            return TestResult.Fail(e.message ?: "Connection failed")
        } finally {
            waitersMutex.withLock { waiters.remove(id) }
        }
    }

    private fun nonce(): String = Random.nextLong().toULong().toString(16)

    private suspend fun queryOnce(query: AnalysisQuery, timeoutMs: Long): AnalysisResponse {
        val deferred = CompletableDeferred<AnalysisResponse>()
        waitersMutex.withLock { waiters[query.id] = QueryWaiter.OneShot(deferred) }
        try {
            awaitOnline()
            sendMutex.withLock {
                sendJson(analysisJson.encodeToString(AnalysisQuery.serializer(), query))
            }
            return try {
                withTimeout(timeoutMs) { deferred.await() }
            } catch (e: TimeoutCancellationException) {
                throw IllegalStateException("Timed out after ${timeoutMs / 1000}s")
            }
        } finally {
            waitersMutex.withLock { waiters.remove(query.id) }
        }
    }

    private suspend fun awaitOnline() {
        if (_status.value.online) return
        withTimeout(12_000) {
            status.first { it.online }
        }
    }

    private suspend fun runConnection(profile: EngineProfile) {
        outbound?.close()
        outbound = null
        inFlight = null
        if (profile.url.isBlank()) {
            _status.value = EngineStatus(EnginePhase.Disconnected)
            _live.value = null
            return
        }
        var backoff = 1_000L
        while (true) {
            _status.value = EngineStatus(EnginePhase.Connecting)
            val channel = Channel<String>(Channel.BUFFERED)
            outbound = channel
            try {
                ws.connect(
                    url = profile.url,
                    token = profile.token,
                    outgoing = channel,
                    onOpen = {
                        backoff = 1_000L
                        _status.value = EngineStatus(EnginePhase.Ready)
                    },
                    onText = { text -> handleFrame(text) },
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _status.value = EngineStatus(
                    EnginePhase.Error,
                    e.message?.take(160) ?: "Connection failed",
                )
            } finally {
                if (outbound === channel) outbound = null
                channel.close()
            }
            delay(backoff)
            backoff = (backoff * 2).coerceAtMost(15_000L)
        }
    }

    private suspend fun handleFrame(text: String) {
        val response = try {
            analysisJson.decodeFromString(AnalysisResponse.serializer(), text)
        } catch (_: Exception) {
            return
        }
        val id = response.id
        if (id != null && (!response.isDuringSearch || response.error != null)) {
            when (val waiter = waitersMutex.withLock { waiters[id] }) {
                is QueryWaiter.OneShot -> waiter.deferred.complete(response)
                is QueryWaiter.Stream -> waiter.channel.trySend(response)
                null -> Unit
            }
        }
        if (response.action != null && response.rootInfo == null) return
        val flight = inFlight ?: return
        if (id != flight.queryId) return
        if (response.error != null) {
            _status.value = EngineStatus(EnginePhase.Ready, response.error)
            return
        }
        if (!flight.publishLive) return
        val root = response.rootInfo ?: return
        val view = toBlackView(root.winrate, root.scoreLead)
        _live.value = LiveAnalysis(
            queryId = flight.queryId,
            sessionId = flight.sessionId,
            nodeId = flight.nodeId,
            blackWinrate = view.winrate,
            blackScoreLead = view.scoreLead,
            visits = root.visits,
            candidates = selectCandidates(response.moveInfos, flight.boardSize, flight.toPlay),
            isDuringSearch = response.isDuringSearch,
            moveInfos = response.moveInfos,
            toPlay = flight.toPlay,
            ownership = response.ownership,
        )
        if (!response.isDuringSearch) {
            _status.value = EngineStatus(EnginePhase.Ready)
        }
    }

    private fun sendJson(text: String) {
        outbound?.trySend(text)
    }

    private data class InFlight(
        val queryId: String,
        val sessionId: String,
        val nodeId: String,
        val boardSize: Int,
        val toPlay: StoneColor,
        val publishLive: Boolean = true,
    )

    private sealed class QueryWaiter {
        class OneShot(val deferred: CompletableDeferred<AnalysisResponse>) : QueryWaiter()
        class Stream(val channel: Channel<AnalysisResponse>) : QueryWaiter()
    }
}
