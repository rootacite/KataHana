package com.acite.katahana.engine

import com.acite.katahana.domain.Point
import com.acite.katahana.domain.Position
import com.acite.katahana.domain.StoneColor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PerspectiveTest {

    @Test
    fun alreadyBlackWinrateIsKeptOnWhiteToPlay() {
        val view = toBlackView(0.0200922408, -2.30880205)
        assertEquals(0.0200922408, view.winrate, 1e-9)
        assertEquals(-2.30880205, view.scoreLead, 1e-9)
    }

    @Test
    fun sideToMoveHelperFlipsOnlyWhenAsked() {
        val flipped = toBlackViewFromSideToMove(0.4, 2.5, StoneColor.White)
        assertEquals(0.6, flipped.winrate, 1e-9)
        assertEquals(-2.5, flipped.scoreLead, 1e-9)
    }

    @Test
    fun selectCandidatesSkipsPassAndKeepsOrder() {
        val json = FakeAnalysisServer.reply(
            analysisJson.encodeToString(AnalysisQuery.serializer(), buildTestQuery("s:n0:live:1")),
        )
        val response = analysisJson.decodeFromString(AnalysisResponse.serializer(), json)
        val top = selectCandidates(response.moveInfos, boardSize = 9, toPlay = StoneColor.Black)
        assertEquals(3, top.size)
        assertEquals(listOf("E5", "F5", "D5"), top.map { it.gtp })
        assertEquals(listOf(0, 1, 3), top.map { it.order })
        assertTrue(top.none { it.gtp.equals("pass", ignoreCase = true) })
        assertEquals(4, top[0].point?.x)
        assertEquals(4, top[0].point?.y)
        assertEquals(listOf("E5", "G5", "F6", "D6"), top[0].pv)
    }

    @Test
    fun ownershipIndexMatchesGtpTopLeftToBottomRight() {
        assertEquals(0, ownershipIndex(Point.fromGtp("A19", 19)!!, 19))
        assertEquals(18, ownershipIndex(Point.fromGtp("T19", 19)!!, 19))
        assertEquals(360, ownershipIndex(Point.fromGtp("T1", 19)!!, 19))
        assertEquals(0, ownershipIndex(Point.fromGtp("A9", 9)!!, 9))
        assertEquals(80, ownershipIndex(Point.fromGtp("J1", 9)!!, 9))
    }

    @Test
    fun heldScalarKeepsPreviousWhenIncomingMissing() {
        assertEquals(0.62, heldScalar(null, 0.62))
        assertEquals(0.41, heldScalar(0.41, 0.62))
        assertEquals(null, heldScalar(null, null))
    }

    @Test
    fun heldOwnershipKeepsPreviousWhenIncomingEmpty() {
        val previous = List(9) { 0.4 }
        assertEquals(previous, heldOwnership(previous, emptyList(), 3))
        assertEquals(emptyList(), heldOwnership(emptyList(), emptyList(), 3))
        val incoming = List(9) { -0.2 }
        assertEquals(incoming, heldOwnership(previous, incoming, 3))
        assertEquals(emptyList(), heldOwnership(previous, emptyList(), 9))
    }

    @Test
    fun lerpOwnershipBlendsCellwise() {
        val from = floatArrayOf(0f, 1f, -1f)
        val to = floatArrayOf(1f, -1f, 1f)
        val mid = lerpOwnership(from, to, 0.5f)
        assertEquals(0.5f, mid[0], 1e-5f)
        assertEquals(0f, mid[1], 1e-5f)
        assertEquals(0f, mid[2], 1e-5f)
        assertEquals(1f, lerpOwnership(from, to, 1f)[0], 1e-5f)
        assertEquals(0f, lerpOwnership(from, to, 0f)[0], 1e-5f)
    }

    @Test
    fun formatPvTruncates() {
        val pv = listOf("E5", "G5", "F6", "D6", "C5")
        assertEquals("E5 G5 F6", formatPv(pv, limit = 3))
        assertEquals("E5 G5 F6 D6 C5", formatPv(pv))
        assertEquals("", formatPv(emptyList()))
    }

    @Test
    fun selectCandidatesDropsMovesPastAcceptableLoss() {
        val infos = listOf(
            move("D4", 0, 5.0),
            move("E4", 1, 4.7),
            move("F4", 2, 3.4),
            move("pass", 3, 4.9),
            move("C4", 4, 2.0),
        )
        val selected = selectCandidates(infos, boardSize = 9, toPlay = StoneColor.Black)
        assertEquals(listOf("D4", "E4"), selected.map { it.gtp })
        assertEquals(0.0, selected[0].pointsLost, 1e-9)
        assertEquals(0.3, selected[1].pointsLost, 1e-9)
    }

    @Test
    fun selectCandidatesUsesWhiteSign() {
        val infos = listOf(
            move("D4", 0, -2.0),
            move("E4", 1, -1.6),
            move("F4", 2, 2.0),
        )
        val selected = selectCandidates(infos, boardSize = 9, toPlay = StoneColor.White)
        assertEquals(listOf("D4", "E4"), selected.map { it.gtp })
        assertEquals(0.0, selected[0].pointsLost, 1e-9)
        assertEquals(0.4, selected[1].pointsLost, 1e-9)
    }

    @Test
    fun selectCandidatesCapsAtTen() {
        val infos = (0 until 15).map { i ->
            move(Point(i % 9, i / 9).toGtp(9), i, 10.0 - i * 0.05)
        }
        val selected = selectCandidates(infos, boardSize = 9, toPlay = StoneColor.Black)
        assertEquals(MAX_CANDIDATES, selected.size)
        assertEquals(0, selected.first().order)
        assertEquals(9, selected.last().order)
    }

    @Test
    fun formatScoreLossMatchesKaTrainDelta() {
        assertEquals("0.0", formatScoreLoss(0.0))
        assertEquals("0.0", formatScoreLoss(0.04))
        assertEquals("-0.3", formatScoreLoss(0.3))
        assertEquals("-1.2", formatScoreLoss(1.24))
        assertEquals("+0.2", formatScoreLoss(-0.16))
    }

    @Test
    fun parseLiveQueryIdReadsSessionAndNode() {
        val parsed = parseLiveQueryId("abc:n3:live:ff")
        assertEquals("abc" to "n3", parsed)
    }

    @Test
    fun classifyDeadMarksBlackInWhiteTerritory() {
        val p = Point.fromGtp("D4", 9)!!
        val pos = Position.of(9, black = listOf(p))
        val ownership = MutableList(81) { 0.0 }
        ownership[p.index(9)] = -0.9
        assertEquals(setOf(p), classifyDead(pos.cells, 9, ownership))
    }

    @Test
    fun classifyDeadMarksWhiteInBlackTerritory() {
        val p = Point.fromGtp("E5", 9)!!
        val pos = Position.of(9, white = listOf(p))
        val ownership = MutableList(81) { 0.0 }
        ownership[p.index(9)] = 0.9
        assertEquals(setOf(p), classifyDead(pos.cells, 9, ownership))
    }

    @Test
    fun classifyDeadIgnoresBelowEnterThreshold() {
        val p = Point.fromGtp("D4", 9)!!
        val pos = Position.of(9, black = listOf(p))
        val ownership = MutableList(81) { 0.0 }
        ownership[p.index(9)] = -0.5
        assertEquals(emptySet(), classifyDead(pos.cells, 9, ownership))
    }

    @Test
    fun classifyDeadStickyOnUnmatchedNodeDoesNotMarkNewStone() {
        val oldDead = Point.fromGtp("A1", 9)!!
        val placed = Point.fromGtp("D4", 9)!!
        val pos = Position.of(9, black = listOf(oldDead, placed))
        val dead = classifyDead(
            cells = pos.cells,
            size = 9,
            ownership = emptyList(),
            previousDead = setOf(oldDead),
            sameNode = false,
        )
        assertEquals(setOf(oldDead), dead)
        assertTrue(placed !in dead)
    }

    @Test
    fun classifyDeadDropsCapturedFromSticky() {
        val oldDead = Point.fromGtp("A1", 9)!!
        val dead = classifyDead(
            cells = Position.empty(9).cells,
            size = 9,
            ownership = emptyList(),
            previousDead = setOf(oldDead),
            sameNode = false,
        )
        assertEquals(emptySet(), dead)
    }

    @Test
    fun classifyDeadMatchingMapMarksNewStoneOnce() {
        val placed = Point.fromGtp("D4", 9)!!
        val pos = Position.of(9, black = listOf(placed))
        val ownership = MutableList(81) { 0.0 }
        ownership[placed.index(9)] = -0.9
        assertEquals(
            setOf(placed),
            classifyDead(pos.cells, 9, ownership, previousDead = emptySet(), sameNode = false),
        )
        ownership[placed.index(9)] = 0.8
        assertEquals(
            emptySet(),
            classifyDead(pos.cells, 9, ownership, previousDead = setOf(placed), sameNode = true),
        )
    }

    @Test
    fun classifyDeadHysteresisOnSameNode() {
        val p = Point.fromGtp("D4", 9)!!
        val pos = Position.of(9, black = listOf(p))
        val ownership = MutableList(81) { 0.0 }
        ownership[p.index(9)] = -0.50
        assertEquals(
            setOf(p),
            classifyDead(pos.cells, 9, ownership, previousDead = setOf(p), sameNode = true),
        )
        assertEquals(
            emptySet(),
            classifyDead(pos.cells, 9, ownership, previousDead = emptySet(), sameNode = false),
        )
        ownership[p.index(9)] = -0.40
        assertEquals(
            emptySet(),
            classifyDead(pos.cells, 9, ownership, previousDead = setOf(p), sameNode = true),
        )
    }

    private fun move(gtp: String, order: Int, scoreLead: Double): MoveInfo = MoveInfo(
        move = gtp,
        order = order,
        visits = 8,
        winrate = 0.5,
        scoreLead = scoreLead,
    )
}
