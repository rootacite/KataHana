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

const val OWNERSHIP_SKIP = 0.08
const val PV_DISPLAY_LEN = 12

data class Candidate(
    val point: Point?,
    val gtp: String,
    val blackWinrate: Double,
    val blackScoreLead: Double,
    val visits: Int,
    val order: Int,
    val pointsLost: Double,
    val pv: List<String> = emptyList(),
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
    val ownership: List<Double> = emptyList(),
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
            pv = info.pv,
        )
    }
    val playable = mapped.filter { !it.gtp.equals("pass", ignoreCase = true) }
    val source = playable.ifEmpty { mapped }
    val acceptable = source.filter { it.pointsLost <= maxPointsLost }
    return (if (acceptable.isNotEmpty()) acceptable else source.take(1)).take(limit)
}

/** KataGo ownership is row-major from top-left (A19) to bottom-right (T1), black-positive. */
fun ownershipIndex(point: Point, size: Int): Int = point.index(size)

fun formatPv(pv: List<String>, limit: Int = PV_DISPLAY_LEN): String =
    pv.take(limit).joinToString(" ")

/** Keep the last complete map when the new node has no eval yet. */
fun heldOwnership(previous: List<Double>, incoming: List<Double>, boardSize: Int): List<Double> {
    val n = boardSize * boardSize
    if (incoming.size == n) return incoming
    return if (previous.size == n) previous else emptyList()
}

fun lerpOwnership(from: FloatArray, to: FloatArray, t: Float): FloatArray {
    val n = minOf(from.size, to.size)
    if (n == 0) return FloatArray(0)
    val u = t.coerceIn(0f, 1f)
    val v = 1f - u
    return FloatArray(n) { i -> from[i] * v + to[i] * u }
}

/** Enemy-territory |ownership| to enter the dead set. High confidence. */
const val DEAD_ENTER = 0.70

/** Enemy-territory |ownership| to leave the dead set. Same node only. */
const val DEAD_LEAVE = 0.45

/** Skip noisy early live packets when marking dead stones. */
const val DEAD_MIN_VISITS = 20

/** How strongly the opponent owns this point. Ownership is black-positive. */
fun enemyOwnership(color: StoneColor, blackPositive: Double): Double =
    if (color == StoneColor.Black) -blackPositive else blackPositive

/**
 * Dead stones from a KataGo ownership map.
 *
 * When [sameNode] is false, only [DEAD_ENTER] applies (jump to a cached eval, or
 * sticky intersect if [ownership] is the wrong length). When true, stones already
 * in [previousDead] stay dead until enemy ownership drops below [DEAD_LEAVE].
 *
 * Never marks a stone from a stale map: pass empty [ownership] after a move so
 * new stones stay out of the set until this node's eval arrives.
 */
fun classifyDead(
    cells: IntArray,
    size: Int,
    ownership: List<Double>,
    previousDead: Set<Point> = emptySet(),
    sameNode: Boolean = false,
): Set<Point> {
    val n = size * size
    if (cells.size != n) return emptySet()
    val onBoard = HashSet<Point>(n)
    for (i in 0 until n) {
        if (StoneColor.fromCell(cells[i]) != null) onBoard += Point.fromIndex(i, size)
    }
    val sticky = previousDead.filterTo(HashSet()) { it in onBoard }
    if (ownership.size != n) return sticky
    val next = HashSet<Point>()
    for (i in 0 until n) {
        val color = StoneColor.fromCell(cells[i]) ?: continue
        val point = Point.fromIndex(i, size)
        val enemy = enemyOwnership(color, ownership[i])
        val wasDead = sameNode && point in sticky
        val threshold = if (wasDead) DEAD_LEAVE else DEAD_ENTER
        if (enemy >= threshold) next += point
    }
    return next
}

