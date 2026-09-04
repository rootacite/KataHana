package com.acite.katahana.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StoneLinksTest {

    @Test
    fun nobiBlocksTobiInTheSameLine() {
        val snap = board(black = listOf(Point(0, 4), Point(1, 4), Point(2, 4)))
        val links = stoneLinks(snap).filter { it.color == StoneColor.Black }
        assertEquals(2, links.count { it.kind == LinkKind.Nobi })
        assertTrue(links.none { it.kind == LinkKind.Tobi })
    }

    @Test
    fun isolatedTobiIsKept() {
        val snap = board(black = listOf(Point(0, 4), Point(2, 4)))
        val links = stoneLinks(snap)
        assertEquals(1, links.size)
        assertEquals(LinkKind.Tobi, links.first().kind)
    }

    @Test
    fun kosumiStaysKosumi() {
        val snap = board(black = listOf(Point(2, 2), Point(3, 3)))
        val links = stoneLinks(snap)
        assertEquals(listOf(LinkKind.Kosumi), links.map { it.kind })
    }

    @Test
    fun kosumiChainDoesNotAddLongerDiagonal() {
        val snap = board(black = listOf(Point(1, 1), Point(2, 2), Point(3, 3)))
        val links = stoneLinks(snap).filter { it.kind == LinkKind.Kosumi }
        assertEquals(2, links.size)
    }

    @Test
    fun keimaIsDroppedWhenATighterStoneSitsInTheBox() {
        val snap = board(black = listOf(Point(2, 2), Point(3, 3), Point(3, 4)))
        val links = stoneLinks(snap)
        assertTrue(links.any { it.kind == LinkKind.Kosumi })
        assertTrue(links.any { it.kind == LinkKind.Nobi })
        assertTrue(links.none { it.kind == LinkKind.Keima })
    }

    @Test
    fun isolatedKeimaIsKept() {
        val snap = board(black = listOf(Point(2, 2), Point(3, 4)))
        val links = stoneLinks(snap)
        assertEquals(1, links.size)
        assertEquals(LinkKind.Keima, links.single().kind)
    }

    @Test
    fun nobiPathSuppressesKosumi() {
        val snap = board(black = listOf(Point(0, 0), Point(1, 0), Point(1, 1)))
        val links = stoneLinks(snap)
        assertEquals(2, links.count { it.kind == LinkKind.Nobi })
        assertTrue(links.none { it.kind == LinkKind.Kosumi })
    }

    @Test
    fun solidTwoByTwoKeepsFourNobiDropsKosumi() {
        val snap = board(black = listOf(Point(0, 0), Point(1, 0), Point(0, 1), Point(1, 1)))
        val links = stoneLinks(snap)
        assertEquals(4, links.count { it.kind == LinkKind.Nobi })
        assertTrue(links.none { it.kind == LinkKind.Kosumi })
    }

    @Test
    fun jumpSquareKeepsFourTobi() {
        val snap = board(black = listOf(Point(0, 0), Point(2, 0), Point(0, 2), Point(2, 2)))
        val links = stoneLinks(snap)
        assertEquals(4, links.count { it.kind == LinkKind.Tobi })
        assertEquals(4, links.size)
    }

    @Test
    fun pinShapeDropsJumpWhenKosumiPathExists() {
        val snap = board(black = listOf(Point(2, 1), Point(1, 2), Point(3, 2)))
        val links = stoneLinks(snap)
        assertTrue(links.none { it.kind == LinkKind.Tobi })
        assertEquals(2, links.count { it.kind == LinkKind.Kosumi })
    }

    @Test
    fun emptyBoardHasNoLinks() {
        assertTrue(stoneLinks(board()).isEmpty())
    }

    private fun board(
        size: Int = 9,
        black: List<Point> = emptyList(),
        white: List<Point> = emptyList(),
    ): SessionSnapshot {
        val pos = Position.of(size, black = black, white = white)
        return SessionSnapshot(
            size = size,
            cells = pos.copyCells(),
            toPlay = StoneColor.Black,
            lastMove = null,
            lastWasPass = false,
            capturedByBlack = 0,
            capturedByWhite = 0,
            canUndo = false,
            canRedo = false,
            ended = false,
            moveNumber = 0,
            komi = 7.5f,
            mode = PlayMode.HumanVsHuman,
            rankKyu = 5,
            humanPlaysBlack = true,
        )
    }
}
