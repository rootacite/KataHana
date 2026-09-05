package com.acite.katahana.ai

import com.acite.katahana.domain.Move
import com.acite.katahana.domain.Point
import com.acite.katahana.domain.Position
import com.acite.katahana.domain.Rules
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.round
import kotlin.random.Random

data class RankDecision(
    val move: Move,
    val nMoves: Int,
    val usedOverride: Boolean,
)

/**
 * KaTrain Calibrated Rank (`RankStrategy`): policy pick, not weaker visits.
 * Query is maxVisits=1 + includePolicy. Sampling uses equal weights on every
 * policy>0 intersection (`generate_weighted_coords` weight=1), then the highest
 * legal policy among the sample.
 */
object RankBot {
    fun nMoves(legalCount: Int, boardSquares: Int, kyu: Int): Int {
        if (legalCount <= 0) return 1
        val norm = legalCount.toDouble() / boardSquares.toDouble()
        val orig = 0.063015 + 0.7624 * boardSquares /
            pow10(-0.05737 * kyu + 1.9482)
        val expTerm = 3.002 * norm * norm - norm - 0.034889 * kyu - 0.5097
        val modified = (0.3931 + 0.6559 * norm * exp(-expTerm * expTerm) - 0.01093 * kyu) * orig
        val denom = 1.31165 * (modified + 1) - 0.082653
        val raw = boardSquares * norm / denom
        // KaTrain: max(1, round(n_moves)) — do not clamp to legal here.
        return maxOf(1, round(raw).toInt())
    }

    fun choose(
        policy: List<Double>,
        position: Position,
        kyu: Int,
        rng: Random,
    ): RankDecision {
        val size = position.size
        val squares = size * size
        val toPlay = position.toPlay
        if (policy.size < squares + 1) {
            val legal = Rules.legalMoves(position)
            val fallback = legal.firstOrNull()?.let { Move.Place(toPlay, it) } ?: Move.Pass(toPlay)
            return RankDecision(fallback, 1, usedOverride = true)
        }
        val passPolicy = policy[squares]
        // KaTrain `policy_ranking`: every intersection + pass, occupied included.
        val ranked = ArrayList<Pair<Double, Point?>>(squares + 1)
        for (i in 0 until squares) {
            ranked += policy[i] to Point.fromIndex(i, size)
        }
        ranked += passPolicy to null
        ranked.sortByDescending { it.first }

        val top5Pass = ranked.take(5).any { it.second == null }
        val legal = Rules.legalMoves(position).toHashSet()
        // KaTrain `generate_weighted_coords`: policy > 0 on the whole grid, not legal-only.
        val policyPositive = ranked.filter { (pol, point) ->
            point != null && pol > 0.0
        }
        val top = ranked.first()
        val topPoint = top.second
        val topIsLegal = topPoint == null || topPoint in legal
        val fillRatio = (squares - policyPositive.size).toDouble() / squares
        val override = 0.8 * (1.0 - 0.5 * fillRatio)
        val override2 = 0.85 + maxOf(0.0, 0.02 * (kyu - 8))
        val second = ranked.getOrNull(1)?.first ?: 0.0
        val forceTop = top5Pass || top.first > override || top.first + second > override2
        if (forceTop && topIsLegal) {
            val move = if (topPoint == null) Move.Pass(toPlay) else Move.Place(toPlay, topPoint)
            return RankDecision(move, nMoves = 1, usedOverride = true)
        }

        if (policyPositive.isEmpty()) {
            return RankDecision(Move.Pass(toPlay), 1, usedOverride = true)
        }

        val n = nMoves(policyPositive.size, squares, kyu)
        val sample = weightedSample(policyPositive, n, rng)
        val bestLegal = sample.filter { it.second in legal }.maxByOrNull { it.first }
        if (bestLegal == null || bestLegal.first < passPolicy) {
            val move = if (topPoint == null || topPoint !in legal) {
                Move.Pass(toPlay)
            } else {
                Move.Place(toPlay, topPoint)
            }
            return RankDecision(move, n, usedOverride = true)
        }
        val point = bestLegal.second ?: return RankDecision(Move.Pass(toPlay), n, usedOverride = false)
        return RankDecision(Move.Place(toPlay, point), n, usedOverride = false)
    }

    /**
     * KaTrain `weighted_selection_without_replacement` with weight=1 (uniform),
     * then the caller takes max policy among the sample.
     */
    internal fun weightedSample(
        items: List<Pair<Double, Point?>>,
        n: Int,
        rng: Random,
    ): List<Pair<Double, Point?>> {
        if (items.isEmpty()) return emptyList()
        val take = n.coerceAtMost(items.size)
        return items
            .map { item ->
                val u = rng.nextDouble().coerceAtLeast(1e-18)
                val key = ln(u) / (1.0 + 1e-18)
                key to item
            }
            .sortedByDescending { it.first }
            .take(take)
            .map { it.second }
    }

    private fun pow10(exp: Double): Double = exp(ln(10.0) * exp)
}
