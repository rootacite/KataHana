package com.acite.katahana.engine

import com.acite.katahana.domain.FORECAST_MAX_PLIES
import com.acite.katahana.domain.GameTree
import com.acite.katahana.domain.Move
import com.acite.katahana.domain.Point
import com.acite.katahana.domain.StoneColor
import com.acite.katahana.domain.humanSlProfile

const val ANALYSIS_PV_LEN = 12
const val FORECAST_PV_LEN = FORECAST_MAX_PLIES - 1

fun StoneColor.toGtp(): String = if (this == StoneColor.Black) "B" else "W"

fun Move.toGtpPair(boardSize: Int): List<String> = when (this) {
    is Move.Place -> listOf(color.toGtp(), point.toGtp(boardSize))
    is Move.Pass -> listOf(color.toGtp(), "pass")
}

fun buildLiveQuery(
    sessionId: String,
    nodeId: String,
    nonce: String,
    tree: GameTree,
    maxVisits: Int,
    reportEverySeconds: Double? = 0.4,
): AnalysisQuery {
    val moves = tree.lineMoves().map { it.toGtpPair(tree.size) }
    return AnalysisQuery(
        id = "$sessionId:$nodeId:live:$nonce",
        rules = tree.rules,
        komi = tree.komi.toDouble(),
        boardXSize = tree.size,
        boardYSize = tree.size,
        moves = moves,
        analyzeTurns = listOf(moves.size),
        maxVisits = maxVisits,
        includeOwnership = true,
        includePolicy = false,
        reportDuringSearchEvery = reportEverySeconds,
        analysisPVLen = ANALYSIS_PV_LEN,
    )
}

fun buildReviewQuery(
    sessionId: String,
    nodeId: String,
    nonce: String,
    tree: GameTree,
    moves: List<Move>,
    maxVisits: Int,
    reportEverySeconds: Double? = 0.4,
): AnalysisQuery {
    val gtpMoves = moves.map { it.toGtpPair(tree.size) }
    return AnalysisQuery(
        id = "$sessionId:$nodeId:review:$nonce",
        rules = tree.rules,
        komi = tree.komi.toDouble(),
        boardXSize = tree.size,
        boardYSize = tree.size,
        moves = gtpMoves,
        analyzeTurns = listOf(gtpMoves.size),
        maxVisits = maxVisits,
        includeOwnership = true,
        includePolicy = false,
        reportDuringSearchEvery = reportEverySeconds,
        analysisPVLen = ANALYSIS_PV_LEN,
    )
}

fun buildRankQuery(
    sessionId: String,
    nodeId: String,
    nonce: String,
    tree: GameTree,
    maxVisits: Int = 1,
): AnalysisQuery {
    val moves = tree.lineMoves().map { it.toGtpPair(tree.size) }
    return AnalysisQuery(
        id = "$sessionId:$nodeId:rank:$nonce",
        rules = tree.rules,
        komi = tree.komi.toDouble(),
        boardXSize = tree.size,
        boardYSize = tree.size,
        moves = moves,
        analyzeTurns = listOf(moves.size),
        maxVisits = maxVisits,
        includeOwnership = true,
        includePolicy = true,
        reportDuringSearchEvery = 0.4,
    )
}

fun buildHumanQuery(
    sessionId: String,
    nodeId: String,
    nonce: String,
    tree: GameTree,
    rankKyu: Int,
    maxVisits: Int = 1,
): AnalysisQuery {
    val moves = tree.lineMoves().map { it.toGtpPair(tree.size) }
    return AnalysisQuery(
        id = "$sessionId:$nodeId:human:$nonce",
        rules = tree.rules,
        komi = tree.komi.toDouble(),
        boardXSize = tree.size,
        boardYSize = tree.size,
        moves = moves,
        analyzeTurns = listOf(moves.size),
        maxVisits = maxVisits,
        includeOwnership = true,
        includePolicy = true,
        reportDuringSearchEvery = 0.4,
        overrideSettings = OverrideSettings(
            reportAnalysisWinratesAs = "BLACK",
            humanSLProfile = humanSlProfile(rankKyu),
            ignorePreRootHistory = false,
        ),
    )
}

fun buildForecastQuery(
    sessionId: String,
    nodeId: String,
    nonce: String,
    tree: GameTree,
    origin: Point,
    maxVisits: Int,
): AnalysisQuery {
    val first = Move.Place(tree.current.position.toPlay, origin)
    val moves = (tree.lineMoves() + first).map { it.toGtpPair(tree.size) }
    return AnalysisQuery(
        id = "$sessionId:$nodeId:forecast:$nonce",
        rules = tree.rules,
        komi = tree.komi.toDouble(),
        boardXSize = tree.size,
        boardYSize = tree.size,
        moves = moves,
        analyzeTurns = listOf(moves.size),
        maxVisits = maxVisits,
        includeOwnership = true,
        includePolicy = false,
        reportDuringSearchEvery = null,
        analysisPVLen = FORECAST_PV_LEN,
    )
}

fun buildForecastOwnershipQuery(
    sessionId: String,
    nodeId: String,
    nonce: String,
    tree: GameTree,
    continuation: List<Move>,
    maxVisits: Int,
): AnalysisQuery {
    val path = tree.lineMoves()
    val moves = (path + continuation).map { it.toGtpPair(tree.size) }
    val pathLen = path.size
    val turns = if (continuation.size <= 1) {
        emptyList()
    } else {
        ((pathLen + 2)..(pathLen + continuation.size)).toList()
    }
    return AnalysisQuery(
        id = "$sessionId:$nodeId:forecast-own:$nonce",
        rules = tree.rules,
        komi = tree.komi.toDouble(),
        boardXSize = tree.size,
        boardYSize = tree.size,
        moves = moves,
        analyzeTurns = turns,
        maxVisits = maxVisits,
        includeOwnership = true,
        includePolicy = false,
        reportDuringSearchEvery = null,
        analysisPVLen = FORECAST_PV_LEN,
    )
}

fun buildGenmoveQuery(
    sessionId: String,
    nodeId: String,
    nonce: String,
    tree: GameTree,
    maxVisits: Int,
): AnalysisQuery {
    val moves = tree.lineMoves().map { it.toGtpPair(tree.size) }
    return AnalysisQuery(
        id = "$sessionId:$nodeId:genmove:$nonce",
        rules = tree.rules,
        komi = tree.komi.toDouble(),
        boardXSize = tree.size,
        boardYSize = tree.size,
        moves = moves,
        analyzeTurns = listOf(moves.size),
        maxVisits = maxVisits,
        includeOwnership = true,
        includePolicy = false,
        reportDuringSearchEvery = 0.4,
    )
}

fun buildTestQuery(id: String): AnalysisQuery = AnalysisQuery(
    id = id,
    rules = "chinese",
    komi = 7.5,
    boardXSize = 9,
    boardYSize = 9,
    moves = emptyList(),
    analyzeTurns = listOf(0),
    maxVisits = 2,
    includeOwnership = false,
    includePolicy = false,
    reportDuringSearchEvery = null,
)

private fun benchBoard(id: String, maxVisits: Int, includePolicy: Boolean): AnalysisQuery = AnalysisQuery(
    id = id,
    rules = "chinese",
    komi = 7.5,
    boardXSize = 19,
    boardYSize = 19,
    moves = emptyList(),
    analyzeTurns = listOf(0),
    maxVisits = maxVisits,
    includeOwnership = true,
    includePolicy = includePolicy,
    reportDuringSearchEvery = null,
)

fun buildBenchPolicyQuery(id: String): AnalysisQuery = benchBoard(id, maxVisits = 1, includePolicy = true)

fun buildBenchSearchQuery(id: String, visits: Int = BENCH_PLAY_VISITS): AnalysisQuery =
    benchBoard(id, maxVisits = visits, includePolicy = false)

fun buildBenchHumanQuery(id: String): AnalysisQuery = AnalysisQuery(
    id = id,
    rules = "chinese",
    komi = 7.5,
    boardXSize = 19,
    boardYSize = 19,
    moves = emptyList(),
    analyzeTurns = listOf(0),
    maxVisits = 1,
    includeOwnership = true,
    includePolicy = true,
    reportDuringSearchEvery = null,
    overrideSettings = OverrideSettings(
        reportAnalysisWinratesAs = "BLACK",
        humanSLProfile = humanSlProfile(5),
        ignorePreRootHistory = false,
    ),
)
