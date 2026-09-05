package com.acite.katahana.ai

import com.acite.katahana.domain.Move
import com.acite.katahana.domain.Point
import com.acite.katahana.domain.Position
import com.acite.katahana.domain.Rules
import kotlin.math.ln
import kotlin.random.Random

/**
 * KataGo Human SL move pick: sample one legal move from `humanPolicy`
 * at temperature 1. No obvious-move override — that would lock fights
 * back onto the superhuman first instinct.
 */
object HumanBot {
    const val MISSING_POLICY =
        "humanPolicy missing — start the engine with -human-model"

    fun choose(
        humanPolicy: List<Double>,
        position: Position,
        rng: Random,
    ): Move {
        val size = position.size
        val squares = size * size
        val toPlay = position.toPlay
        if (humanPolicy.size < squares + 1) return Move.Pass(toPlay)
        val legal = Rules.legalMoves(position).toHashSet()
        val candidates = ArrayList<Pair<Double, Move>>(legal.size + 1)
        for (i in 0 until squares) {
            val weight = humanPolicy[i]
            if (weight <= 0.0) continue
            val point = Point.fromIndex(i, size)
            if (point in legal) candidates += weight to Move.Place(toPlay, point)
        }
        val passWeight = humanPolicy[squares]
        if (passWeight > 0.0 && position.consecutivePasses < 2) {
            candidates += passWeight to Move.Pass(toPlay)
        }
        if (candidates.isEmpty()) {
            return legal.firstOrNull()?.let { Move.Place(toPlay, it) } ?: Move.Pass(toPlay)
        }
        return weightedPick(candidates, rng)
    }

    internal fun weightedPick(
        items: List<Pair<Double, Move>>,
        rng: Random,
    ): Move {
        val picked = items
            .map { item ->
                val u = rng.nextDouble().coerceAtLeast(1e-18)
                val weight = item.first.coerceAtLeast(1e-18)
                ln(u) / weight to item.second
            }
            .maxBy { it.first }
        return picked.second
    }
}
