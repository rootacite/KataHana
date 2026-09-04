package com.acite.katahana.engine

import com.acite.katahana.domain.GameTree
import com.acite.katahana.domain.Move
import com.acite.katahana.domain.StoneColor

const val ANALYSIS_PV_LEN = 12

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
