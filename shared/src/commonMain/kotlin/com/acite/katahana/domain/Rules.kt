package com.acite.katahana.domain

object Rules {
    fun liberties(position: Position, point: Point): Int {
        if (!point.inBounds(position.size)) return 0
        val cell = position.cells[point.index(position.size)]
        if (cell == StoneColor.EMPTY_CELL) return 0
        return groupAndLiberties(position.cells, position.size, point.index(position.size)).second.size
    }

    fun groupPoints(position: Position, point: Point): Set<Point> {
        if (!point.inBounds(position.size)) return emptySet()
        val cell = position.cells[point.index(position.size)]
        if (cell == StoneColor.EMPTY_CELL) return emptySet()
        return groupAndLiberties(position.cells, position.size, point.index(position.size))
            .first
            .map { Point.fromIndex(it, position.size) }
            .toSet()
    }

    fun tryPlay(position: Position, point: Point): PlayResult {
        if (position.consecutivePasses >= 2) return PlayResult.Illegal(IllegalReason.GameOver)
        if (!point.inBounds(position.size)) return PlayResult.Illegal(IllegalReason.OutOfBounds)
        val idx = point.index(position.size)
        if (position.cells[idx] != StoneColor.EMPTY_CELL) {
            return PlayResult.Illegal(IllegalReason.Occupied)
        }

        val color = position.toPlay
        val opponent = color.opponent().cell()
        val next = position.copyCells()
        next[idx] = color.cell()

        val captured = ArrayList<Point>()
        val seen = HashSet<Int>()
        for (n in neighborIndices(idx, position.size)) {
            if (next[n] != opponent || n in seen) continue
            val (group, libs) = groupAndLiberties(next, position.size, n)
            seen.addAll(group)
            if (libs.isEmpty()) {
                for (g in group) {
                    next[g] = StoneColor.EMPTY_CELL
                    captured.add(Point.fromIndex(g, position.size))
                }
            }
        }

        val ownLibs = groupAndLiberties(next, position.size, idx).second
        if (ownLibs.isEmpty()) {
            return PlayResult.Illegal(IllegalReason.Suicide)
        }

        val newHash = hashBoard(next)
        if (newHash in position.history) {
            return PlayResult.Illegal(IllegalReason.Superko)
        }

        val capturedByBlack = position.capturedByBlack + if (color == StoneColor.Black) captured.size else 0
        val capturedByWhite = position.capturedByWhite + if (color == StoneColor.White) captured.size else 0

        return PlayResult.Ok(
            position = Position(
                size = position.size,
                cells = next,
                toPlay = color.opponent(),
                capturedByBlack = capturedByBlack,
                capturedByWhite = capturedByWhite,
                history = position.history + newHash,
                consecutivePasses = 0,
            ),
            captured = captured,
        )
    }

    fun tryPass(position: Position): PlayResult {
        if (position.consecutivePasses >= 2) return PlayResult.Illegal(IllegalReason.GameOver)
        return PlayResult.Ok(
            position = Position(
                size = position.size,
                cells = position.copyCells(),
                toPlay = position.toPlay.opponent(),
                capturedByBlack = position.capturedByBlack,
                capturedByWhite = position.capturedByWhite,
                history = position.history,
                consecutivePasses = position.consecutivePasses + 1,
            ),
            captured = emptyList(),
        )
    }

    fun legalMoves(position: Position): List<Point> {
        if (position.consecutivePasses >= 2) return emptyList()
        val out = ArrayList<Point>()
        for (i in position.cells.indices) {
            if (position.cells[i] != StoneColor.EMPTY_CELL) continue
            val p = Point.fromIndex(i, position.size)
            if (tryPlay(position, p) is PlayResult.Ok) out.add(p)
        }
        return out
    }

    private fun groupAndLiberties(
        cells: IntArray,
        size: Int,
        start: Int,
    ): Pair<Set<Int>, Set<Int>> {
        val color = cells[start]
        val group = LinkedHashSet<Int>()
        val libs = LinkedHashSet<Int>()
        val stack = ArrayDeque<Int>()
        stack.add(start)
        while (stack.isNotEmpty()) {
            val i = stack.removeFirst()
            if (!group.add(i)) continue
            for (n in neighborIndices(i, size)) {
                when (cells[n]) {
                    StoneColor.EMPTY_CELL -> libs.add(n)
                    color -> if (n !in group) stack.add(n)
                }
            }
        }
        return group to libs
    }
}
