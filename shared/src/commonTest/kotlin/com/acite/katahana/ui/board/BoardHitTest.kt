package com.acite.katahana.ui.board

import com.acite.katahana.domain.Point
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class BoardHitTest {
    private val layout = BoardLayout(
        canvasW = 900f,
        canvasH = 900f,
        boardSize = 9,
        showCoords = false,
    )

    @Test
    fun snapsToExactIntersection() {
        val c = layout.center(Point(4, 4))
        assertEquals(Point(4, 4), nearestIntersection(c.x, c.y, layout, TAP_MAX_GAPS))
    }

    @Test
    fun tapRadiusCoversTheCellNotOnlyTheStone() {
        val a = layout.center(Point(2, 3))
        val b = layout.center(Point(3, 3))
        val midX = (a.x + b.x) / 2f
        val hit = nearestIntersection(midX, a.y, layout, TAP_MAX_GAPS)
        assertNotNull(hit)
        assertEquals(3, hit.y)
        assertEquals(true, hit.x == 2 || hit.x == 3)
    }

    @Test
    fun oldStoneRadiusMissesTheMidCell() {
        val a = layout.center(Point(2, 3))
        val b = layout.center(Point(3, 3))
        val midX = (a.x + b.x) / 2f
        assertNull(nearestIntersection(midX, a.y, layout, 0.45f))
    }

    @Test
    fun justOutsideTheGridStillHitsAnEdgePoint() {
        val origin = layout.center(Point(0, 0))
        val x = origin.x - layout.gap * 0.40f
        assertEquals(Point(0, 0), nearestIntersection(x, origin.y, layout, TAP_MAX_GAPS))
    }

    @Test
    fun farOffTheBoardIsIgnored() {
        val c = layout.center(Point(4, 4))
        assertNull(nearestIntersection(c.x, c.y + layout.gap * 8f, layout, SLIDE_MAX_GAPS))
        assertNull(nearestIntersection(c.x - layout.gap * 8f, c.y, layout, TAP_MAX_GAPS))
    }

    @Test
    fun slideRadiusIsWiderThanTap() {
        val origin = layout.center(Point(0, 0))
        val x = origin.x - layout.gap * 0.70f
        assertNull(nearestIntersection(x, origin.y, layout, TAP_MAX_GAPS))
        assertEquals(Point(0, 0), nearestIntersection(x, origin.y, layout, SLIDE_MAX_GAPS))
    }
}
