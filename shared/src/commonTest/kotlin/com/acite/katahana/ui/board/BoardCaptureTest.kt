package com.acite.katahana.ui.board

import com.acite.katahana.domain.Point
import com.acite.katahana.domain.Position
import com.acite.katahana.domain.StoneColor
import kotlin.test.Test
import kotlin.test.assertEquals

class BoardCaptureTest {

    @Test
    fun vanishedStonesFindsCapturedGroup() {
        val size = 9
        val taken = Point(0, 0)
        val previous = Position.of(size, white = listOf(taken)).cells
        val current = Position.empty(size).cells
        val vanished = vanishedStones(previous, current, size)
        assertEquals(listOf(DepartingStone(taken, StoneColor.White)), vanished)
    }

    @Test
    fun vanishedStonesIgnoresLivingAndEmpty() {
        val size = 9
        val keep = Point(1, 1)
        val cells = Position.of(size, black = listOf(keep)).cells
        assertEquals(emptyList(), vanishedStones(cells, cells, size))
    }
}
