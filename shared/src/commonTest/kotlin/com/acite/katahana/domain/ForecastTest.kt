package com.acite.katahana.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ForecastTest {

    @Test
    fun originOnlyPlacesOneVirtualStone() {
        val start = Position.empty(9)
        val origin = Point(4, 4)
        val line = buildForecastLine(start, origin, emptyList())
        assertEquals(listOf(Move.Place(StoneColor.Black, origin)), line.continuation)
        assertEquals(1, line.plies.size)
        val ply = line.plies.single()
        assertEquals(1, ply.ply)
        assertEquals(listOf(ForecastStone(origin, StoneColor.Black, 1)), ply.stones)
        assertTrue(ply.captured.isEmpty())
    }

    @Test
    fun pvAlternatesColorsAndKeepsPlyNumbers() {
        val start = Position.empty(9)
        val origin = Point(4, 4)
        val line = buildForecastLine(start, origin, listOf("C5", "G5"))
        assertEquals(3, line.plies.size)
        assertEquals(StoneColor.Black, line.continuation[0].color)
        assertEquals(StoneColor.White, line.continuation[1].color)
        assertEquals(StoneColor.Black, line.continuation[2].color)
        val last = line.plies.last()
        assertEquals(3, last.stones.size)
        assertEquals(listOf(1, 2, 3), last.stones.map { it.ply })
        assertEquals(Point(4, 4), last.stones[0].point)
        assertEquals(Point.fromGtp("C5", 9), last.stones[1].point)
        assertEquals(Point.fromGtp("G5", 9), last.stones[2].point)
    }

    @Test
    fun captureMarksRealStoneAndDropsCapturedVirtual() {
        val start = Position.of(
            size = 9,
            black = listOf(Point(1, 0)),
            white = listOf(Point(0, 0)),
            toPlay = StoneColor.Black,
        )
        val origin = Point(0, 1)
        val line = buildForecastLine(start, origin, emptyList())
        val ply = line.plies.single()
        assertEquals(setOf(Point(0, 0)), ply.captured)
        assertEquals(listOf(ForecastStone(origin, StoneColor.Black, 1)), ply.stones)
        assertEquals(null, start.stoneAt(origin))
    }

    @Test
    fun passCountsAsAPlyButOccupiesNoPoint() {
        val start = Position.empty(9)
        val origin = Point(4, 4)
        val line = buildForecastLine(start, origin, listOf("pass"))
        assertEquals(2, line.plies.size)
        assertTrue(line.continuation[1] is Move.Pass)
        assertEquals(line.plies[0].stones, line.plies[1].stones)
        assertTrue(line.plies[1].captured.isEmpty())
    }

    @Test
    fun illegalPvStepTruncatesTheLine() {
        val start = Position.empty(9)
        val origin = Point(4, 4)
        val line = buildForecastLine(start, origin, listOf("E5"))
        assertEquals(1, line.plies.size)
        assertEquals(1, line.continuation.size)
    }

    @Test
    fun lineStopsAtSixteenPlies() {
        val start = Position.empty(9)
        val origin = Point(0, 0)
        val pv = ArrayList<String>()
        var pos = (Rules.tryPlay(start, origin) as PlayResult.Ok).position
        repeat(20) {
            val next = Rules.legalMoves(pos).first()
            pv += next.toGtp(9)
            pos = (Rules.tryPlay(pos, next) as PlayResult.Ok).position
        }
        val line = buildForecastLine(start, origin, pv)
        assertEquals(FORECAST_MAX_PLIES, line.plies.size)
        assertEquals(FORECAST_MAX_PLIES, line.continuation.size)
    }

    @Test
    fun capturedVirtualStoneLeavesTheOverlay() {
        val start = Position.empty(9)
        val origin = Point(1, 0)
        val line = buildForecastLine(
            start,
            origin,
            listOf("A9", "A8"),
        )
        assertEquals(3, line.plies.size)
        val afterCapture = line.plies.last()
        assertTrue(afterCapture.stones.none { it.point == Point(0, 0) })
        assertTrue(afterCapture.stones.any { it.point == origin && it.color == StoneColor.Black })
        assertTrue(afterCapture.stones.any { it.point == Point(0, 1) && it.color == StoneColor.Black })
    }

    @Test
    fun revealRequiresOwnershipOfThatPly() {
        val ply = ForecastPly(1, emptyList(), emptySet())
        val forecast = Forecast(Point(0, 0), "n0", listOf(ply))
        assertFalse(canRevealForecastPly(forecast, 1, 81))
        val ready = forecast.withPlyOwnership(1, List(81) { 0.0 })
        assertTrue(canRevealForecastPly(ready, 1, 81))
        assertFalse(canRevealForecastPly(ready, 2, 81))
    }

    @Test
    fun illegalOriginYieldsEmptyLine() {
        val start = Position.of(9, black = listOf(Point(4, 4)))
        val line = buildForecastLine(start, Point(4, 4), listOf("C3"))
        assertTrue(line.continuation.isEmpty())
        assertTrue(line.plies.isEmpty())
    }
}
