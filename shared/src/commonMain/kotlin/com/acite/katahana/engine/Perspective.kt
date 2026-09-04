package com.acite.katahana.engine

import com.acite.katahana.domain.Point
import com.acite.katahana.domain.StoneColor

data class BlackView(
    val winrate: Double,
    val scoreLead: Double,
)

/**
 * This gateway (KaTrain analysis_config) reports winrates as BLACK.
 * Queries also send overrideSettings.reportAnalysisWinratesAs = BLACK.
 * Do not flip by side-to-move — that would invert the bar on White's turn.
 */
fun toBlackView(winrate: Double, scoreLead: Double): BlackView =
    BlackView(winrate, scoreLead)

fun toBlackViewFromSideToMove(
    winrate: Double,
    scoreLead: Double,
    toPlay: StoneColor,
): BlackView =
    if (toPlay == StoneColor.Black) {
        BlackView(winrate, scoreLead)
    } else {
        BlackView(1.0 - winrate, -scoreLead)
    }

const val ACCEPTABLE_POINTS_LOST = 1.5
const val MAX_CANDIDATES = 10

data class Candidate(
    val point: Point?,
    val gtp: String,
    val blackWinrate: Double,
    val blackScoreLead: Double,
    val visits: Int,
    val order: Int,
    val pointsLost: Double,
)

data class LiveAnalysis(
    val queryId: String,
    val sessionId: String,
    val nodeId: String,
    val blackWinrate: Double,
    val blackScoreLead: Double,
    val visits: Int,
    val candidates: List<Candidate>,
    val isDuringSearch: Boolean,
    val moveInfos: List<MoveInfo> = emptyList(),
    val toPlay: StoneColor = StoneColor.Black,
)

fun parseLiveQueryId(id: String): Pair<String, String>? {
    val parts = id.split(':')
    if (parts.size < 4) return null
    if (parts[parts.size - 2] != "live") return null
    val nodeId = parts[parts.size - 3]
    val sessionId = parts.dropLast(3).joinToString(":")
    return sessionId to nodeId
}

/**
 * Side-to-move points lost versus the engine's best move (order 0).
 * [scoreLead] is always Black's lead.
 */
fun pointsLost(
    bestScoreLead: Double,
    moveScoreLead: Double,
    toPlay: StoneColor,
): Double {
    val sign = if (toPlay == StoneColor.Black) 1.0 else -1.0
    return sign * (bestScoreLead - moveScoreLead)
}

/** KaTrain-style delta: 0.0 for the best, negative when worse. */
fun formatScoreLoss(pointsLost: Double): String {
    val tenths = kotlin.math.round(-pointsLost * 10.0).toInt()
    if (tenths == 0) return "0.0"
    val sign = if (tenths > 0) "+" else "-"
    val abs = kotlin.math.abs(tenths)
    return "$sign${abs / 10}.${abs % 10}"
}

fun selectCandidates(
    moveInfos: List<MoveInfo>,
    boardSize: Int,
    toPlay: StoneColor,
    maxPointsLost: Double = ACCEPTABLE_POINTS_LOST,
    limit: Int = MAX_CANDIDATES,
): List<Candidate> {
    if (moveInfos.isEmpty()) return emptyList()
    val ranked = moveInfos.sortedBy { it.order }
    val bestLead = ranked.first().scoreLead
    val mapped = ranked.map { info ->
        Candidate(
            point = Point.fromGtp(info.move, boardSize),
            gtp = info.move,
            blackWinrate = info.winrate,
            blackScoreLead = info.scoreLead,
            visits = info.visits,
            order = info.order,
            pointsLost = pointsLost(bestLead, info.scoreLead, toPlay),
        )
    }
    val playable = mapped.filter { !it.gtp.equals("pass", ignoreCase = true) }
    val source = playable.ifEmpty { mapped }
    val acceptable = source.filter { it.pointsLost <= maxPointsLost }
    return (if (acceptable.isNotEmpty()) acceptable else source.take(1)).take(limit)
}
