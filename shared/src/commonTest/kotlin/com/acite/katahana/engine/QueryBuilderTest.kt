package com.acite.katahana.engine

import com.acite.katahana.domain.GameTree
import com.acite.katahana.domain.Point
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QueryBuilderTest {

    @Test
    fun emptyBoardAnalyzesTurnZero() {
        val tree = GameTree(9)
        val query = buildLiveQuery("sess", tree.current.id, "1", tree, maxVisits = 8)
        assertEquals(emptyList(), query.moves)
        assertEquals(listOf(0), query.analyzeTurns)
        assertEquals(9, query.boardXSize)
        assertEquals(9, query.boardYSize)
        assertEquals("chinese", query.rules)
        assertEquals(7.5, query.komi, 1e-6)
        assertTrue(query.id.contains(":live:"))
        assertTrue(query.id.startsWith("sess:${tree.current.id}:live:"))
        assertEquals(true, query.includeOwnership)
        assertEquals(false, query.includePolicy)
        assertEquals(12, query.analysisPVLen)
        assertEquals("BLACK", query.overrideSettings.reportAnalysisWinratesAs)
    }

    @Test
    fun reviewQueryUsesPathMovesAndReviewPurpose() {
        val tree = GameTree(9)
        tree.play(Point(4, 4))
        val node = tree.current
        tree.play(Point(3, 3))
        val currentId = tree.current.id
        val moves = tree.pathMoves(node)
        val query = buildReviewQuery("sess", node.id, "ab", tree, moves, maxVisits = 400)
        assertTrue(query.id.contains(":review:"))
        assertTrue(query.id.startsWith("sess:${node.id}:review:"))
        assertEquals(true, query.includeOwnership)
        assertEquals(12, query.analysisPVLen)
        assertEquals(400, query.maxVisits)
        assertEquals(listOf(listOf("B", "E5")), query.moves)
        assertEquals(listOf(1), query.analyzeTurns)
        assertEquals(currentId, tree.current.id)
    }

    @Test
    fun rankQueryAsksPolicyAtOneVisit() {
        val tree = GameTree(9)
        val query = buildRankQuery("sess", tree.current.id, "1", tree)
        assertTrue(query.id.contains(":rank:"))
        assertEquals(true, query.includePolicy)
        assertEquals(true, query.includeOwnership)
        assertEquals(1, query.maxVisits)
        assertEquals(listOf(0), query.analyzeTurns)
        assertEquals(0.4, query.reportDuringSearchEvery)
    }

    @Test
    fun genmoveQueryUsesPlayVisits() {
        val tree = GameTree(9)
        val query = buildGenmoveQuery("sess", tree.current.id, "1", tree, maxVisits = 400)
        assertTrue(query.id.contains(":genmove:"))
        assertEquals(400, query.maxVisits)
        assertEquals(false, query.includePolicy)
        assertEquals(true, query.includeOwnership)
        assertEquals(0.4, query.reportDuringSearchEvery)
    }

    @Test
    fun gtpSkipsIAndRecordsPass() {
        val tree = GameTree(19)
        tree.play(Point(8, 0))
        tree.pass()
        val query = buildLiveQuery("s", tree.current.id, "n", tree, 400)
        assertEquals(
            listOf(listOf("B", "J19"), listOf("W", "pass")),
            query.moves,
        )
        assertEquals(listOf(2), query.analyzeTurns)
    }
}
