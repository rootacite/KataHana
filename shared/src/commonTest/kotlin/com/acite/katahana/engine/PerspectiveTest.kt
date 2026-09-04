package com.acite.katahana.engine

import com.acite.katahana.domain.Point
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

    private fun move(gtp: String, order: Int, scoreLead: Double): MoveInfo = MoveInfo(
        move = gtp,
        order = order,
        visits = 8,
        winrate = 0.5,
        scoreLead = scoreLead,
    )
}
