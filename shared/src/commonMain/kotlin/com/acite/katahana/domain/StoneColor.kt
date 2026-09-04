package com.acite.katahana.domain

enum class StoneColor {
    Black,
    White,
    ;

    fun opponent(): StoneColor = if (this == Black) White else Black

    fun cell(): Int = if (this == Black) BLACK_CELL else WHITE_CELL

    companion object {
        const val EMPTY_CELL = 0
        const val BLACK_CELL = 1
        const val WHITE_CELL = 2

        fun fromCell(cell: Int): StoneColor? = when (cell) {
            BLACK_CELL -> Black
            WHITE_CELL -> White
            else -> null
        }
    }
}
