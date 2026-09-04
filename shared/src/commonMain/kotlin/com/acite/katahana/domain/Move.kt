package com.acite.katahana.domain

sealed class Move {
    abstract val color: StoneColor

    data class Place(override val color: StoneColor, val point: Point) : Move()
    data class Pass(override val color: StoneColor) : Move()
}

enum class IllegalReason {
    Occupied,
    Suicide,
    Superko,
    GameOver,
    OutOfBounds,
}

sealed class PlayResult {
    data class Ok(val position: Position, val captured: List<Point>) : PlayResult()
    data class Illegal(val reason: IllegalReason) : PlayResult()
}
