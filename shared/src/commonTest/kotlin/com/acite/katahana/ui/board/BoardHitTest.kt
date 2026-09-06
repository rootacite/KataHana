package com.acite.katahana.ui.board

import com.acite.katahana.domain.Point
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

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

    @Test
    fun coordFontFitsInsideGapOnDensePhoneBoard() {
        val dense = BoardLayout(
            canvasW = 1080f,
            canvasH = 1080f,
            boardSize = 19,
            showCoords = true,
        )
        val fontPx = coordFontPx(dense.gap)
        assertTrue(fontPx < dense.gap)
        assertEquals(dense.gap * COORD_BAND_GAPS, dense.coordBand)
    }

    @Test
    fun woodMarginIsAFractionOfGapNotBoardSide() {
        val plain = BoardLayout(1080f, 1080f, 19, showCoords = false)
        assertEquals(plain.gap * BOARD_MARGIN_GAPS, plain.inset)
        assertTrue(plain.inset / plain.side < 0.04f)
        val labelled = BoardLayout(1080f, 1080f, 19, showCoords = true)
        assertEquals(labelled.gap * (BOARD_MARGIN_GAPS + COORD_BAND_GAPS), labelled.inset)
        assertTrue(labelled.inset > plain.inset)
        assertTrue(labelled.inset / labelled.side < 0.065f)
        assertTrue(coordFontPx(labelled.gap) <= labelled.coordBand)
    }

    @Test
    fun coordBandScalesWithGap() {
        val small = BoardLayout(320f, 320f, 19, showCoords = true)
        val large = BoardLayout(1080f, 1080f, 19, showCoords = true)
        assertTrue(small.coordBand < large.coordBand)
        assertEquals(small.gap * COORD_BAND_GAPS, small.coordBand)
        assertTrue(coordFontPx(small.gap) < small.gap)
    }

    @Test
    fun thinningKeepsEndsAndEvenIndexes() {
        assertTrue(coordsNeedThinning(20f, 18f))
        assertFalse(coordsNeedThinning(12f, 18f))
        assertTrue(shouldDrawCoordIndex(0, 19, thin = true))
        assertTrue(shouldDrawCoordIndex(18, 19, thin = true))
        assertTrue(shouldDrawCoordIndex(2, 19, thin = true))
        assertFalse(shouldDrawCoordIndex(1, 19, thin = true))
        assertTrue(shouldDrawCoordIndex(1, 19, thin = false))
    }
}
