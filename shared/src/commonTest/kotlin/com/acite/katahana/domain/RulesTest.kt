package com.acite.katahana.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RulesTest {

    @Test
    fun singleStoneHasFourLiberties() {
        val tree = GameTree(9)
        assertIs<PlayResult.Ok>(tree.play(Point(4, 4)))
        assertEquals(4, Rules.liberties(tree.current.position, Point(4, 4)))
    }

    @Test
    fun groupSharesLiberties() {
        val tree = GameTree(9)
        tree.play(Point(4, 4))
        tree.play(Point(0, 0))
        tree.play(Point(4, 5))
        assertEquals(6, Rules.liberties(tree.current.position, Point(4, 4)))
        assertEquals(6, Rules.liberties(tree.current.position, Point(4, 5)))
        assertEquals(2, Rules.groupPoints(tree.current.position, Point(4, 4)).size)
    }

    @Test
    fun cornerStoneHasTwoLiberties() {
        val tree = GameTree(9)
        tree.play(Point(0, 0))
        assertEquals(2, Rules.liberties(tree.current.position, Point(0, 0)))
    }

    @Test
    fun captureRemovesGroupAndCounts() {
        val tree = GameTree(9)
        tree.play(Point(1, 0))
        tree.play(Point(0, 0))
        val result = tree.play(Point(0, 1))
        val ok = assertIs<PlayResult.Ok>(result)
        assertEquals(1, ok.captured.size)
        assertEquals(Point(0, 0), ok.captured.single())
        assertEquals(null, tree.current.position.stoneAt(0, 0))
        assertEquals(1, tree.current.position.capturedByBlack)
        assertEquals(0, tree.current.position.capturedByWhite)
    }

    @Test
    fun suicideIsIllegal() {
        val pos = Position.of(
            size = 9,
            white = listOf(Point(1, 0), Point(0, 1), Point(2, 1), Point(1, 2)),
            toPlay = StoneColor.Black,
        )
        val result = Rules.tryPlay(pos, Point(1, 1))
        val illegal = assertIs<PlayResult.Illegal>(result)
        assertEquals(IllegalReason.Suicide, illegal.reason)
    }

    @Test
    fun capturingIsNotSuicide() {
        val pos = Position.of(
            size = 9,
            black = listOf(Point(1, 0)),
            white = listOf(Point(0, 0)),
            toPlay = StoneColor.Black,
        )
        val result = Rules.tryPlay(pos, Point(0, 1))
        val ok = assertIs<PlayResult.Ok>(result)
        assertEquals(listOf(Point(0, 0)), ok.captured)
        assertEquals(null, ok.position.stoneAt(0, 0))
        assertEquals(StoneColor.Black, ok.position.stoneAt(0, 1))
    }

    @Test
    fun simpleKoForbidsImmediateRecapture() {
        val pos = Position.of(
            size = 9,
            black = listOf(Point(1, 0), Point(0, 1), Point(1, 2)),
            white = listOf(Point(2, 0), Point(1, 1), Point(3, 1), Point(2, 2)),
            toPlay = StoneColor.Black,
        )
        val afterTake = assertIs<PlayResult.Ok>(Rules.tryPlay(pos, Point(2, 1)))
        assertEquals(listOf(Point(1, 1)), afterTake.captured)
        val recapture = Rules.tryPlay(afterTake.position, Point(1, 1))
        val illegal = assertIs<PlayResult.Illegal>(recapture)
        assertEquals(IllegalReason.Superko, illegal.reason)
    }

    @Test
    fun koIsLegalAfterATenuki() {
        val pos = Position.of(
            size = 9,
            black = listOf(Point(1, 0), Point(0, 1), Point(1, 2)),
            white = listOf(Point(2, 0), Point(1, 1), Point(3, 1), Point(2, 2)),
            toPlay = StoneColor.Black,
        )
        val afterTake = assertIs<PlayResult.Ok>(Rules.tryPlay(pos, Point(2, 1)))
        val tenuki = assertIs<PlayResult.Ok>(Rules.tryPlay(afterTake.position, Point(8, 8)))
        val blackTenuki = assertIs<PlayResult.Ok>(Rules.tryPlay(tenuki.position, Point(8, 0)))
        val recapture = Rules.tryPlay(blackTenuki.position, Point(1, 1))
        assertIs<PlayResult.Ok>(recapture)
    }

    @Test
    fun superkoRejectsEarlierBoardNotJustImmediateKo() {
        val future = assertIs<PlayResult.Ok>(
            Rules.tryPlay(
                Position.of(size = 9, black = listOf(Point(4, 4)), toPlay = StoneColor.White),
                Point(5, 5),
            ),
        )
        val now = Position.of(
            size = 9,
            black = listOf(Point(4, 4)),
            toPlay = StoneColor.White,
            extraHistory = listOf(future.position.hash),
        )
        val result = Rules.tryPlay(now, Point(5, 5))
        val illegal = assertIs<PlayResult.Illegal>(result)
        assertEquals(IllegalReason.Superko, illegal.reason)
        assertTrue(now.hash != future.position.hash)
    }

    @Test
    fun occupiedPointIsIllegal() {
        val tree = GameTree(9)
        tree.play(Point(3, 3))
        val result = tree.play(Point(3, 3))
        val illegal = assertIs<PlayResult.Illegal>(result)
        assertEquals(IllegalReason.Occupied, illegal.reason)
    }

    @Test
    fun twoPassesEndTheGame() {
        val tree = GameTree(9)
        assertIs<PlayResult.Ok>(tree.pass())
        assertIs<PlayResult.Ok>(tree.pass())
        assertTrue(tree.ended)
        val third = tree.play(Point(4, 4))
        assertEquals(IllegalReason.GameOver, (third as PlayResult.Illegal).reason)
    }

    @Test
    fun boardSizesNineThirteenNineteen() {
        for (size in listOf(9, 13, 19)) {
            val tree = GameTree(size)
            val center = Point(size / 2, size / 2)
            assertIs<PlayResult.Ok>(tree.play(center), "size $size")
            assertEquals(StoneColor.Black, tree.current.position.stoneAt(center))
        }
    }

    @Test
    fun gtpSkipsIAndCountsFromBottom() {
        assertEquals("A19", Point(0, 0).toGtp(19))
        assertEquals("A1", Point(0, 18).toGtp(19))
        assertEquals("J1", Point(8, 18).toGtp(19))
        assertEquals("Q16", Point(15, 3).toGtp(19))
        assertEquals(Point(15, 3), Point.fromGtp("Q16", 19))
        assertEquals(Point(8, 18), Point.fromGtp("J1", 19))
    }
}
