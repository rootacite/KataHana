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
    fun isolatedTobiHasNoLean() {
        val snap = board(black = listOf(Point(0, 4), Point(2, 4)))
        val link = stoneLinks(snap).single()
        assertEquals(LinkKind.Tobi, link.kind)
        assertEquals(0, link.lean)
    }

    @Test
    fun jumpSquareTobiLeansOutward() {
        val snap = board(black = listOf(Point(0, 0), Point(2, 0), Point(0, 2), Point(2, 2)))
        val tobi = stoneLinks(snap).filter { it.kind == LinkKind.Tobi }
        assertEquals(4, tobi.size)
        for (link in tobi) {
            val mid = link.via.first()
            val dx = (link.b.x - link.a.x).toFloat()
            val dy = (link.b.y - link.a.y).toFloat()
            val nx = dy * link.lean
            val ny = -dx * link.lean
            val towardCenter = nx * (1f - mid.x) + ny * (1f - mid.y)
            assertTrue(link.lean != 0, "edge at $mid should arch")
            assertTrue(towardCenter < 0f, "edge at $mid should lean away from (1,1), lean=${link.lean}")
        }
    }

    @Test
    fun isolatedKeimaKeepsOneElbow() {
        val snap = board(black = listOf(Point(2, 2), Point(3, 4)))
        val link = stoneLinks(snap).single()
        assertEquals(LinkKind.Keima, link.kind)
        assertEquals(1, link.via.size)
    }

    @Test
    fun keimaBesideABlockPicksTheOuterElbow() {
        val snap = board(
            black = listOf(
                Point(1, 1), Point(2, 1),
                Point(1, 2), Point(2, 2),
                Point(3, 4),
            ),
        )
        val keima = stoneLinks(snap).single { it.kind == LinkKind.Keima }
        assertEquals(setOf(Point(2, 2), Point(3, 4)), setOf(keima.a, keima.b))
        assertEquals(Point(2, 4), keima.via.first())
    }

    @Test
    fun keimaRingPicksOuterElbows() {
        val snap = board(black = listOf(Point(2, 2), Point(4, 3), Point(3, 5), Point(1, 4)))
        val keima = stoneLinks(snap).filter { it.kind == LinkKind.Keima }
        assertEquals(4, keima.size)
        assertEquals(
            setOf(Point(4, 2), Point(4, 5), Point(1, 5), Point(1, 2)),
            keima.map { it.via.first() }.toSet(),
        )
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
        )
    }
}
