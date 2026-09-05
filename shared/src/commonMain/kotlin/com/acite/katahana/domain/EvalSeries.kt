package com.acite.katahana.domain

import com.acite.katahana.ai.QualityBand
import com.acite.katahana.ai.QualityMark
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

enum class EvalGraphMode {
    Score,
    Winrate,
}

data class EvalSample(
    val moveNumber: Int,
    val blackWinrate: Double,
    val blackScoreLead: Double,
    val nodeId: String,
)

fun EvalSample.advantage(mode: EvalGraphMode): Double = when (mode) {
    EvalGraphMode.Score -> blackScoreLead
    EvalGraphMode.Winrate -> (blackWinrate - 0.5) * 100.0
}

/** Root-info snapshot used to plot a node. [hasView] is false when only a loss was persisted. */
data class EvalView(
    val blackWinrate: Double,
    val blackScoreLead: Double,
    val hasView: Boolean = true,
)

fun samplesFrom(
    line: List<Node>,
    evalOf: (nodeId: String) -> EvalView?,
): List<EvalSample> {
    val out = ArrayList<EvalSample>(line.size)
    for (node in line) {
        val view = evalOf(node.id) ?: continue
        if (!view.hasView) continue
        out += EvalSample(
            moveNumber = node.moveNumber,
            blackWinrate = view.blackWinrate,
            blackScoreLead = view.blackScoreLead,
            nodeId = node.id,
        )
    }
    return out
}

fun niceAbs(maxAbs: Double, floor: Double): Double {
    val raw = max(abs(maxAbs) * 1.15, floor)
    if (!raw.isFinite() || raw <= 0.0) return floor
    val mag = 10.0.pow(floor(log10(raw)))
    val n = raw / mag
    val nice = when {
        n <= 1.0 -> 1.0
        n <= 1.5 -> 1.5
        n <= 2.0 -> 2.0
        n <= 3.0 -> 3.0
        n <= 5.0 -> 5.0
        else -> 10.0
    }
    return nice * mag
}

fun yMaxFor(samples: List<EvalSample>, mode: EvalGraphMode): Double {
    val floor = if (mode == EvalGraphMode.Score) 5.0 else 10.0
    val peak = samples.maxOfOrNull { abs(it.advantage(mode)) } ?: 0.0
    return niceAbs(peak, floor)
}

val STAT_BANDS: List<QualityBand> = listOf(
    QualityBand.Good,
    QualityBand.Fair,
    QualityBand.Inaccuracy,
    QualityBand.Mistake,
    QualityBand.BigMistake,
    QualityBand.Blunder,
)

data class QualityStats(
    val black: Map<QualityBand, Int> = emptyMap(),
    val white: Map<QualityBand, Int> = emptyMap(),
) {
    fun count(color: StoneColor, band: QualityBand): Int {
        val map = if (color == StoneColor.Black) black else white
        return map[band] ?: 0
    }
}

fun qualityCounts(marks: List<QualityMark>): QualityStats {
    val black = HashMap<QualityBand, Int>(STAT_BANDS.size)
    val white = HashMap<QualityBand, Int>(STAT_BANDS.size)
    for (mark in marks) {
        if (mark.band == QualityBand.Shallow) continue
        val map = if (mark.color == StoneColor.Black) black else white
        map[mark.band] = (map[mark.band] ?: 0) + 1
    }
    return QualityStats(black, white)
}
