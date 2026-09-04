package com.acite.katahana.domain

class Position(
    val size: Int,
    cells: IntArray,
    val toPlay: StoneColor,
    val capturedByBlack: Int,
    val capturedByWhite: Int,
    val history: List<Long>,
    val consecutivePasses: Int,
) {
    val cells: IntArray = cells
    val hash: Long = hashBoard(cells)

    fun stoneAt(point: Point): StoneColor? {
        if (!point.inBounds(size)) return null
        return StoneColor.fromCell(cells[point.index(size)])
    }

    fun stoneAt(x: Int, y: Int): StoneColor? = stoneAt(Point(x, y))

    fun copyCells(): IntArray = cells.copyOf()

    companion object {
        fun empty(size: Int): Position {
            require(size == 9 || size == 13 || size == 19) { "Board size must be 9, 13, or 19" }
            val cells = IntArray(size * size)
            val hash = hashBoard(cells)
            return Position(
                size = size,
                cells = cells,
                toPlay = StoneColor.Black,
                capturedByBlack = 0,
                capturedByWhite = 0,
                history = listOf(hash),
                consecutivePasses = 0,
            )
        }

        fun of(
            size: Int,
            black: Collection<Point> = emptyList(),
            white: Collection<Point> = emptyList(),
            toPlay: StoneColor = StoneColor.Black,
            capturedByBlack: Int = 0,
            capturedByWhite: Int = 0,
            extraHistory: List<Long> = emptyList(),
            consecutivePasses: Int = 0,
        ): Position {
            val cells = IntArray(size * size)
            for (p in black) cells[p.index(size)] = StoneColor.BLACK_CELL
            for (p in white) cells[p.index(size)] = StoneColor.WHITE_CELL
            val hash = hashBoard(cells)
            return Position(
                size = size,
                cells = cells,
                toPlay = toPlay,
                capturedByBlack = capturedByBlack,
                capturedByWhite = capturedByWhite,
                history = extraHistory + hash,
                consecutivePasses = consecutivePasses,
            )
        }
    }
}

internal fun hashBoard(cells: IntArray): Long {
    var h = -7046029254386353131L
    for (c in cells) {
        h = h * 31 + c
    }
    return h
}

internal fun neighborIndices(index: Int, size: Int): IntArray {
    val x = index % size
    val y = index / size
    val out = IntArray(4)
    var n = 0
    if (x > 0) out[n++] = index - 1
    if (x < size - 1) out[n++] = index + 1
    if (y > 0) out[n++] = index - size
    if (y < size - 1) out[n++] = index + size
    return if (n == 4) out else out.copyOf(n)
}
