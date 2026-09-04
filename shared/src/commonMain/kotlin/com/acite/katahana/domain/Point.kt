package com.acite.katahana.domain

data class Point(val x: Int, val y: Int) {
    fun index(size: Int): Int = y * size + x

    fun inBounds(size: Int): Boolean = x in 0 until size && y in 0 until size

    /**
     * GTP / KataGo coordinate: columns A–T skipping I, rows 1-indexed from the bottom.
     * [y] is 0 at the top of the board (canvas row).
     */
    fun toGtp(boardSize: Int): String {
        val letterIndex = if (x >= 8) x + 1 else x
        val col = ('A' + letterIndex)
        val row = boardSize - y
        return "$col$row"
    }

    companion object {
        fun fromIndex(index: Int, size: Int): Point = Point(index % size, index / size)

        fun fromGtp(coord: String, boardSize: Int): Point? {
            if (coord.equals("pass", ignoreCase = true)) return null
            if (coord.length < 2) return null
            val colChar = coord[0].uppercaseChar()
            if (colChar == 'I') return null
            val raw = colChar - 'A'
            val x = if (colChar > 'I') raw - 1 else raw
            val row = coord.substring(1).toIntOrNull() ?: return null
            val y = boardSize - row
            val point = Point(x, y)
            return point.takeIf { it.inBounds(boardSize) }
        }
    }
}

fun hoshiPoints(size: Int): List<Point> = when (size) {
    9 -> listOf(Point(2, 2), Point(6, 2), Point(2, 6), Point(6, 6), Point(4, 4))
    13 -> listOf(Point(3, 3), Point(9, 3), Point(3, 9), Point(9, 9), Point(6, 6))
    19 -> {
        val t = listOf(3, 9, 15)
        t.flatMap { y -> t.map { x -> Point(x, y) } }
    }
    else -> emptyList()
}
