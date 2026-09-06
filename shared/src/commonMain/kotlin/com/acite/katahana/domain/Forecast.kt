package com.acite.katahana.domain

const val FORECAST_MAX_PLIES = 16

data class ForecastStone(
    val point: Point,
    val color: StoneColor,
    val ply: Int,
)

data class ForecastPly(
    val ply: Int,
    val stones: List<ForecastStone>,
    val captured: Set<Point>,
    val ownership: List<Double> = emptyList(),
)

data class ForecastLine(
    val origin: Point,
    val continuation: List<Move>,
    val plies: List<ForecastPly>,
)

data class Forecast(
    val origin: Point,
    val nodeId: String,
    val plies: List<ForecastPly>,
    val originLoss: Double? = null,
) {
    fun withPlyOwnership(ply: Int, ownership: List<Double>): Forecast = copy(
        plies = plies.map { if (it.ply == ply) it.copy(ownership = ownership) else it },
    )

    fun revealed(upToPly: Int): ForecastPly? =
        plies.lastOrNull { it.ply <= upToPly && it.ply > 0 }
}

fun buildForecastLine(
    start: Position,
    origin: Point,
    pv: List<String>,
    maxPlies: Int = FORECAST_MAX_PLIES,
): ForecastLine {
    val first = Rules.tryPlay(start, origin)
    if (first !is PlayResult.Ok) {
        return ForecastLine(origin, emptyList(), emptyList())
    }
    val continuation = ArrayList<Move>(maxPlies)
    val positions = ArrayList<Position>(maxPlies)
    continuation += Move.Place(start.toPlay, origin)
    positions += first.position

    var pos = first.position
    for (gtp in pv) {
        if (continuation.size >= maxPlies) break
        val color = pos.toPlay
        val pass = gtp.equals("pass", ignoreCase = true)
        val result = if (pass) {
            Rules.tryPass(pos)
        } else {
            val point = Point.fromGtp(gtp, start.size) ?: break
            Rules.tryPlay(pos, point)
        }
        if (result !is PlayResult.Ok) break
        continuation += if (pass) {
            Move.Pass(color)
        } else {
            Move.Place(color, Point.fromGtp(gtp, start.size)!!)
        }
        pos = result.position
        positions += pos
        if (pos.consecutivePasses >= 2) break
    }

    val lastPlace = LinkedHashMap<Point, ForecastStone>()
    val plies = ArrayList<ForecastPly>(positions.size)
    for (i in positions.indices) {
        when (val move = continuation[i]) {
            is Move.Place -> lastPlace[move.point] = ForecastStone(move.point, move.color, i + 1)
            is Move.Pass -> Unit
        }
        val after = positions[i]
        val stones = lastPlace.values.filter { placed ->
            after.stoneAt(placed.point) == placed.color &&
                start.stoneAt(placed.point) != placed.color
        }.sortedBy { it.ply }
        val captured = LinkedHashSet<Point>()
        val n = start.size * start.size
        for (idx in 0 until n) {
            val p = Point.fromIndex(idx, start.size)
            if (start.stoneAt(p) != null && after.stoneAt(p) == null) captured += p
        }
        plies += ForecastPly(ply = i + 1, stones = stones, captured = captured)
    }
    return ForecastLine(origin, continuation, plies)
}

fun canRevealForecastPly(forecast: Forecast, ply: Int, boardSquares: Int): Boolean {
    val data = forecast.plies.getOrNull(ply - 1) ?: return false
    return data.ownership.size == boardSquares
}
